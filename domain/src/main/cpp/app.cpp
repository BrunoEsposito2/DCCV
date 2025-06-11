#include <opencv2/opencv.hpp>
#include <opencv2/videoio.hpp>
#include <opencv2/objdetect.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/imgproc.hpp>
#include <iostream>
#include <iomanip>
#include <websocketpp/config/asio_no_tls.hpp>
#include <websocketpp/server.hpp>
#include <thread>
#include <chrono>
#include <atomic>
#include <filesystem>
#include <random>
#include <string>

#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <unistd.h>

#include <sys/select.h>

typedef websocketpp::server<websocketpp::config::asio> Server;
using namespace cv;
using namespace std;
using websocketpp::connection_hdl;
namespace fs = std::filesystem;

int manager_socket = -1;

class Detector {
    enum Mode { Face, Body } m;
    HOGDescriptor hog;
    CascadeClassifier face_cascade;
    Rect detectionWindow;
    bool useWindow;

    bool loadCascadeClassifier() {
        vector<string> possiblePaths = {
            "haarcascade_frontalface_default.xml",
            "/usr/local/share/opencv4/haarcascades/haarcascade_frontalface_default.xml",
            "/usr/share/opencv4/haarcascades/haarcascade_frontalface_default.xml",
            "/usr/share/opencv/haarcascades/haarcascade_frontalface_default.xml"
        };

        for (const auto& path : possiblePaths) {
            if (fs::exists(path)) {
                if (face_cascade.load(path)) {
                    cout << "Successfully loaded cascade classifier from: " << path << endl;
                    return true;
                }
            }
        }

        cerr << "Error: Could not find or load the face cascade classifier file." << endl;
        cerr << "Please ensure the file is in one of these locations:" << endl;
        for (const auto& path : possiblePaths) {
            cerr << "  - " << path << endl;
        }
        return false;
    }

public:
    Detector(int x = 0, int y = 0, int width = 0, int height = 0) : m(Face), hog() {
        hog.setSVMDetector(HOGDescriptor::getDefaultPeopleDetector());
        detectionWindow = Rect(x, y, width, height);
        useWindow = (width > 0 && height > 0);

        if(!loadCascadeClassifier()) {
            throw runtime_error("Cannot load face cascade classifier. Using body detection only.");
            m = Body;
        }
    }

    void toggleMode() { m = (m == Face ? Body : Face); }
    string modeName() const { return (m == Face ? "Face" : "Body"); }

    vector<Rect> detect(InputArray img) {
        vector<Rect> found;
        Mat gray;

        if (useWindow) {
            // Verifica che la finestra sia all'interno dei limiti del frame
            Rect safeWindow = detectionWindow;
            safeWindow.x = min(max(0, safeWindow.x), img.cols() - 1);
            safeWindow.y = min(max(0, safeWindow.y), img.rows() - 1);
            safeWindow.width = min(safeWindow.width, img.cols() - safeWindow.x);
            safeWindow.height = min(safeWindow.height, img.rows() - safeWindow.y);
        
            if (safeWindow.width <= 0 || safeWindow.height <= 0) {
                // Finestra non valida, usa l'intero frame
                useWindow = false;
                cout << "Invalid detection window. Using full frame." << endl;
            } else {
                // Usa solo la regione specificata
                Mat roi = img.getMat()(safeWindow);
                cvtColor(roi, gray, COLOR_BGR2GRAY);
                equalizeHist(gray, gray);

                if (m == Face && !face_cascade.empty()) {
                    face_cascade.detectMultiScale(gray, found, 1.1, 3, 0, Size(30, 30));
                } else {
                    hog.detectMultiScale(roi, found, 0, Size(8,8), Size(), 1.05, 2, false);
                }

                /*for (auto& r : found) {
                    r.x += safeWindow.x;
                    r.y += safeWindow.y;
                }*/

                return found;
            }
        }

        cvtColor(img, gray, COLOR_BGR2GRAY);
        equalizeHist(gray, gray);

        if (m == Face && !face_cascade.empty()) {
            face_cascade.detectMultiScale(
                gray,
                found,
                1.1,
                3,
                0,
                Size(30, 30)
            );
        } else {
            hog.detectMultiScale(img, found, 0, Size(8,8), Size(), 1.05, 2, false);
        }
        return found;
    }

    void adjustRect(Rect & r) const {
        if (m == Body) {
            r.x += cvRound(r.width*0.1);
            r.width = cvRound(r.width*0.8);
            r.y += cvRound(r.height*0.07);
            r.height = cvRound(r.height*0.8);
        } else {
            r.x -= cvRound(r.width*0.1);
            r.width = cvRound(r.width*1.2);
            r.y -= cvRound(r.height*0.1);
            r.height = cvRound(r.height*1.2);
        }
    }
};

string generateRandomId(int length) {
    const string chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    random_device rd;
    mt19937 generator(rd());
    uniform_int_distribution<> distribution(0, chars.size() - 1);

    string randomId;
    for (int i = 0; i < length; ++i) {
        randomId += chars[distribution(generator)];
    }
    return randomId;
}

class VideoServer {
    string cameraId;
    int windowX, windowY, windowWidth, windowHeight;
    bool useWindow;
public:
    VideoServer(int camera, string file, string id = "",
                int x = 0, int y = 0, int width = 0, int height = 0)
        : running(false), windowX(x), windowY(y),
          windowWidth(width), windowHeight(height) {

        useWindow = (width > 0 && height > 0);
        cameraId = id.empty() ? generateRandomId(10) : id;

        server.init_asio();
        server.set_access_channels(websocketpp::log::alevel::none);
        server.clear_access_channels(websocketpp::log::alevel::all);

        //  per abilitare il riutilizzo dell'indirizzo
        server.set_reuse_addr(true);

        server.set_socket_init_handler([](websocketpp::connection_hdl hdl, boost::asio::ip::tcp::socket& s) {
            try {
                if (s.is_open()) {
                    boost::asio::ip::tcp::no_delay option(true);
                    boost::system::error_code ec;
                    s.set_option(option, ec);
                    if (ec) {
                        std::cerr << "Error setting TCP_NODELAY: " << ec.message() << std::endl;
                    }
                }
            } catch (const std::exception& e) {
                std::cerr << "Exception in socket_init_handler: " << e.what() << std::endl;
            }
        });

        server.set_validate_handler([this](connection_hdl hdl) -> bool {
            auto con = server.get_con_from_hdl(hdl);
            string resource = con->get_resource();
            return resource == "/camera" + this->cameraId;
        });

        server.set_open_handler(bind(&VideoServer::on_open, this, placeholders::_1));
        server.set_close_handler(bind(&VideoServer::on_close, this, placeholders::_1));

        videoThread = thread(&VideoServer::processVideo, this, camera, file);
    }

    // Aggiunto il distruttore
    ~VideoServer() {
        cout << "VideoServer destructor called. Cleaning up resources..." << endl;
        stop();
    }

    string getCameraId() const { return cameraId; }

    bool isPortAvailable(uint16_t port) {
        using namespace websocketpp::lib::asio;
        
        try {
            io_service ios;
            ip::tcp::acceptor acceptor(ios);
    
            // Configure the acceptor
            acceptor.open(ip::tcp::v4());
            
            // Important: set SO_REUSEADDR BEFORE binding
            acceptor.set_option(ip::tcp::acceptor::reuse_address(true));
    
            ip::tcp::endpoint endpoint(ip::tcp::v4(), port);
            
            acceptor.bind(endpoint);
            
            acceptor.close();
            return true;
        } catch(const std::exception& e) {
            cerr << "Port " << port << " check failed: " << e.what() << endl;
            return false;
        }
    }

    void run(uint16_t port) {
        int retry_count = 0;
        const int max_retries = 3;
        
        while (retry_count < max_retries) {
            try {
                // First try to check if the port is available
                if (!isPortAvailable(port)) {
                    std::cout << "Port " << port << " is in use, waiting before retry..." << std::endl;
                    std::this_thread::sleep_for(std::chrono::seconds(2));
                    retry_count++;
                    continue;
                }
                
                // Configure the server
                server.listen(port);
                std::cout << "Server listening on port " << port << std::endl;
                server.start_accept();
                std::cout << "Server started accepting connections" << std::endl;
                
                // Run the server
                server.run();
                break; // If successful, exit the loop
            } catch (const websocketpp::exception& e) {
                std::cerr << "WebSocket server error: " << e.what() << std::endl;
                
                // If this is the last retry, rethrow
                if (retry_count >= max_retries - 1) {
                    throw;
                }
                
                // Otherwise, wait and retry
                std::cout << "Retrying in 2 seconds... (attempt " << (retry_count + 1) << "/" << max_retries << ")" << std::endl;
                std::this_thread::sleep_for(std::chrono::seconds(2));
                retry_count++;
            } catch (const std::exception& e) {
                std::cerr << "Server error: " << e.what() << std::endl;
                throw;
            }
        }
    }

    void stop() {
        if (running.exchange(false)) {
            cout << "Stopping video processing thread..." << endl;
            
            if (videoThread.joinable()) {
                videoThread.join();
                cout << "Video processing thread stopped successfully" << endl;
            }
            
            // Chiudi tutte le connessioni
            closeAllConnections();
            
            // Ferma il server WebSocket
            try {
                cout << "Stopping WebSocket server..." << endl;
                server.stop_listening();
                server.stop();
                cout << "WebSocket server stopped successfully" << endl;
                std::this_thread::sleep_for(std::chrono::milliseconds(500));
            } catch (const std::exception& e) {
                cerr << "Error stopping WebSocket server: " << e.what() << endl;
            }
        }
    }

private:
    void closeAllConnections() {
        lock_guard<mutex> lock(connectionsMutex);
        cout << "Closing " << connections.size() << " active connections..." << endl;
        
        for (auto it = connections.begin(); it != connections.end(); /* no increment */) {
            try {
                auto con = server.get_con_from_hdl(*it);
                if (con) {
                    con->close(websocketpp::close::status::going_away, "Server shutting down");
                }
                it = connections.erase(it);
            } catch (const std::exception& e) {
                cerr << "Error closing connection: " << e.what() << endl;
                ++it;
            }
        }
        
        cout << "All connections closed" << endl;
    }

    void on_open(connection_hdl hdl) {
        lock_guard<mutex> lock(connectionsMutex);
        connections.insert(hdl);
        cout << "Client connected. Total clients: " << connections.size() << endl;
    }

    void on_close(connection_hdl hdl) {
        lock_guard<mutex> lock(connectionsMutex);
        connections.erase(hdl);
        cout << "Client disconnected. Total clients: " << connections.size() << endl;
    }

    void processVideo(int camera, string file) {
        VideoCapture cap;
        if (file.empty())
            cap.open(camera);
        else {
            file = samples::findFileOrKeep(file);
            cap.open(file);
        }

        if (!cap.isOpened()) {
            cerr << "Cannot open video stream: '" << (file.empty() ? "<camera>" : file) << "'" << endl;
            exit(1);
        } else {
            cout << "Server started successfully" << endl;
            cout.flush();
        }

        double fps = cap.get(CAP_PROP_FPS);
        int delay = 1000 / (fps > 0 ? fps : 30);

        cout << "Press Ctrl+C to quit." << endl;
        cout << "Streaming on WebSocket..." << endl;

        Detector detector(windowX, windowY, windowWidth, windowHeight);
        Mat frame;
        running = true;

        while (running) {
            cap >> frame;
            if (frame.empty()) {
                if (!file.empty()) {
                    cap.set(CAP_PROP_POS_FRAMES, 0);
                    continue;
                }
                break;
            }

            int64 t = getTickCount();
            vector<Rect> found = detector.detect(frame);
            t = getTickCount() - t;

            double fps = getTickFrequency() / (double)t;

            // Invia i dati al CameraManager se connesso
            if (manager_socket >= 0) {
                std::string dataToSend = std::to_string(found.size()) + ":" + 
                                        detector.modeName() + ":" + 
                                        std::to_string(fps) + "\n";
                send(manager_socket, dataToSend.c_str(), dataToSend.length(), 0);
            }

            for (auto& r : found) {
                detector.adjustRect(r);
                if (useWindow) {
                    // Se usiamo la finestra, aggiungi l'offset per la visualizzazione
                    Rect displayRect = r;
                    displayRect.x += windowX;
                    displayRect.y += windowY;
                    rectangle(frame, displayRect.tl(), displayRect.br(), Scalar(0, 255, 0), 2);
                } else {
                    // Comportamento originale
                    rectangle(frame, r.tl(), r.br(), Scalar(0, 255, 0), 2);
                }
            }

            Mat resized;
            resize(frame, resized, Size(), 0.5, 0.5);
            vector<uchar> buffer;
            vector<int> params = {IMWRITE_JPEG_QUALITY, 60};
            imencode(".jpg", resized, buffer, params);

            lock_guard<mutex> lock(connectionsMutex);
            for (auto& hdl : connections) {
                try {
                    server.send(hdl, buffer.data(), buffer.size(), websocketpp::frame::opcode::binary);
                } catch (const websocketpp::exception& e) {
                    cerr << "Send error: " << e.what() << endl;
                }
            }

            this_thread::sleep_for(chrono::milliseconds(delay));
        }

        cout << "Video capture loop terminated" << endl;
        cap.release();
    }

    Server server;
    set<connection_hdl, owner_less<connection_hdl>> connections;
    mutex connectionsMutex;
    thread videoThread;
    atomic<bool> running;
};

// Funzione di gestione del segnale per la chiusura pulita
VideoServer* globalServerPtr = nullptr;

void signalHandler(int signal) {
    cout << "Signal " << signal << " received. Shutting down immediately..." << endl;
    
    // Chiudi la socket del manager se aperta
    if (manager_socket >= 0) {
        close(manager_socket);
        manager_socket = -1;
    }

    if (globalServerPtr) {
        globalServerPtr->stop();
    }
    
    // Short delay to allow resources to be released
    std::this_thread::sleep_for(std::chrono::seconds(1));
    
    // Termina immediatamente
    _exit(0);
}

bool isPortAvailable(uint16_t port) {
    using namespace websocketpp::lib::asio;
    
    try {
        io_service ios;
        ip::tcp::acceptor acceptor(ios);

        // Configure the acceptor
        acceptor.open(ip::tcp::v4());
        
        // Important: set SO_REUSEADDR BEFORE binding
        acceptor.set_option(ip::tcp::acceptor::reuse_address(true));

        ip::tcp::endpoint endpoint(ip::tcp::v4(), port);
        
        acceptor.bind(endpoint);
        
        acceptor.close();
        return true;
    } catch(const std::exception& e) {
        cerr << "Port " << port << " check failed: " << e.what() << endl;
        return false;
    }
}

void managerSocketListener() {
    if (manager_socket < 0) {
        return;
    }
    
    char buffer[1024];
    while (true) {
        // Preparazione per select()
        fd_set readSet;
        FD_ZERO(&readSet);
        FD_SET(manager_socket, &readSet);
        
        // Timeout di 0.5 secondi
        struct timeval timeout;
        timeout.tv_sec = 0;
        timeout.tv_usec = 500000;  // 500ms
        
        // Utilizzo di select per attendere dati senza bloccare
        int activity = select(manager_socket + 1, &readSet, NULL, NULL, &timeout);
        
        if (activity < 0) {
            cout << "Errore nella funzione select(): " << strerror(errno) << endl;
            break;
        }
        
        // Verifica se ci sono dati da leggere
        if (activity > 0 && FD_ISSET(manager_socket, &readSet)) {
            memset(buffer, 0, sizeof(buffer));
            int bytesRead = recv(manager_socket, buffer, sizeof(buffer) - 1, 0);
            
            if (bytesRead <= 0) {
                // Connessione chiusa o errore
                cout << "Connessione con CameraManager interrotta." << endl;
                break;
            }
            
            // Verifica se è stato ricevuto il carattere 'k'
            for (int i = 0; i < bytesRead; i++) {
                if (buffer[i] == 'k') {
                    cout << "Ricevuto comando di terminazione 'k'. Chiusura forzata in corso..." << endl;
                    
                    // Chiudi la socket se non è già stata chiusa
                    if (manager_socket >= 0) {
                        close(manager_socket);
                        manager_socket = -1;
                    }
                    
                    // Termina immediatamente il processo con _exit (bypassa tutti i cleanup)
                    _exit(0);
                }
            }
        }
    }

    cout << "Disconnessione dal CameraManager rilevata. Chiusura in corso..." << endl;
    
    if (manager_socket >= 0) {
        close(manager_socket);
        manager_socket = -1;
    }

    std::this_thread::sleep_for(std::chrono::seconds(3));

    _exit(0);
}

// Funzione per connettersi al CameraManager con tentativi multipli
bool connectToCameraManager(const std::string& host, int port) {
    const int MAX_ATTEMPTS = 5;
    int attempts = 0;
    
    while (attempts < MAX_ATTEMPTS) {
        std::cout << "Tentativo di connessione al CameraManager " << (attempts + 1) << "/" << MAX_ATTEMPTS << std::endl;
        
        int sock = socket(AF_INET, SOCK_STREAM, 0);
        if (sock < 0) {
            std::cerr << "Socket creation failed" << std::endl;
            attempts++;
            std::this_thread::sleep_for(std::chrono::seconds(2));
            continue;
        }
        
        struct sockaddr_in serv_addr;
        serv_addr.sin_family = AF_INET;
        serv_addr.sin_port = htons(port);
        
        if (inet_pton(AF_INET, host.c_str(), &serv_addr.sin_addr) <= 0) {
            std::cerr << "Invalid address / Address not supported" << std::endl;
            close(sock);
            attempts++;
            std::this_thread::sleep_for(std::chrono::seconds(2));
            continue;
        }
        
        if (connect(sock, (struct sockaddr*)&serv_addr, sizeof(serv_addr)) < 0) {
            std::cerr << "Connection attempt " << (attempts + 1) << " failed: " << strerror(errno) << std::endl;
            close(sock);
            attempts++;
            std::this_thread::sleep_for(std::chrono::seconds(2));
            continue;
        }
        
        std::cout << "Successfully connected to CameraManager at " << host << ":" << port << std::endl;
        
        // Memorizza il socket per l'uso successivo
        manager_socket = sock;

        // Avvia un thread per monitorare i dati in arrivo dalla socket
        std::thread listenerThread(managerSocketListener);
        listenerThread.detach();

        return true;
    }
    
    std::cerr << "Failed to connect to CameraManager after " << MAX_ATTEMPTS << " attempts" << std::endl;
    return false;
}

// Use example of x, y, h and w parameters: --x=320 --y=180 --width=600 --height=320
int main(int argc, char** argv) {
    CommandLineParser parser(argc, argv,
        "{ help h   |   | print help message }"
        "{ camera c | 0 | capture video from camera (device index starting from 0) }"
        "{ video v  |   | use video as input }"
        "{ port p   | 5555 | WebSocket server port }"
        "{ id      |   | camera identifier for the stream endpoint }"
        "{ x       | 0 | x coordinate of detection window }"
        "{ y       | 0 | y coordinate of detection window }"
        "{ width w | 0 | width of detection window (0 for full frame) }"
        "{ height h| 0 | height of detection window (0 for full frame) }");

    parser.about("Face/Body detection with WebSocket streaming capability");

    if (parser.has("help")) {
        parser.printMessage();
        return 0;
    }

    int camera = parser.get<int>("camera");
    string file = parser.get<string>("video");
    int port = parser.get<int>("port");
    string cameraId = parser.get<string>("id");

    // Parametri opzionali della finestra
    int x = parser.get<int>("x");
    int y = parser.get<int>("y");
    int width = parser.get<int>("width");
    int height = parser.get<int>("height");

    if (!parser.check()) {
        parser.printErrors();
        return 1;
    }
    
    try {
        // Check if port is available before creating the server
        if (!isPortAvailable(port)) {
            // Se abbiamo l'opzione SO_REUSEADDR, possiamo continuare anche se la porta sembra in uso
            cerr << "Warning: Port " << port << " might still be in TIME_WAIT state, trying to reuse it..." << endl;
            // Continuiamo comunque, dato che abbiamo impostato SO_REUSEADDR nel server
        }

        // Connetti al CameraManager
        std::string managerHost = "127.0.0.1";
        int managerPort = 8080;
        std::thread connectionThread([managerHost, managerPort]() {
            // Ritarda leggermente la connessione per dare tempo al CameraManager di prepararsi
            std::this_thread::sleep_for(std::chrono::seconds(1));
            connectToCameraManager(managerHost, managerPort);
        });
        connectionThread.detach();
        
        unique_ptr<VideoServer> server = make_unique<VideoServer>(
            camera, file, cameraId, x, y, width, height);
        
        // Registra il gestore di segnali per la chiusura pulita
        globalServerPtr = server.get();
        signal(SIGINT, signalHandler);
        signal(SIGTERM, signalHandler);
        
        cout << "Video server started on port " << port << endl;
        cout << "Stream available at: /camera" << server->getCameraId() << endl;
        if (width > 0 && height > 0) {
            cout << "Using detection window: x=" << x << ", y=" << y
                    << ", width=" << width << ", height=" << height << endl;
        }
        
        try {
            // Run the server
            server->run(port);
        } catch (const websocketpp::exception& e) {
            cerr << "WebSocket server error during run: " << e.what() << endl;
            // Assicuriamoci di terminare completamente anche in caso di errore
            if (globalServerPtr) {
                globalServerPtr->stop();
            }
            return 1;
        }
        
        // Dopo run(), il server è stato fermato, rilascia il puntatore globale
        globalServerPtr = nullptr;
    } catch (const websocketpp::exception& e) {
        cerr << "WebSocket server error: " << e.what() << endl;
        return 1;
    } catch (const exception& e) {
        cerr << "Error: " << e.what() << endl;
        return 1;
    }

    return 0;
}