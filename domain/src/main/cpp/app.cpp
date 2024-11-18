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

typedef websocketpp::server<websocketpp::config::asio> Server;
using namespace cv;
using namespace std;
using websocketpp::connection_hdl;
namespace fs = std::filesystem;

class Detector {
    enum Mode { Face, Body } m;
    HOGDescriptor hog;
    CascadeClassifier face_cascade;

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
    Detector() : m(Face), hog() {
        hog.setSVMDetector(HOGDescriptor::getDefaultPeopleDetector());

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

class VideoServer {
public:
    VideoServer(int camera, string file) : running(false) {
        server.init_asio();
        server.set_access_channels(websocketpp::log::alevel::none);
        server.clear_access_channels(websocketpp::log::alevel::all);

        server.set_open_handler(bind(&VideoServer::on_open, this, placeholders::_1));
        server.set_close_handler(bind(&VideoServer::on_close, this, placeholders::_1));

        videoThread = thread(&VideoServer::processVideo, this, camera, file);
    }

    void run(uint16_t port) {
        server.listen(port);
        server.start_accept();
        try {
            server.run();
        } catch (const exception& e) {
            cerr << "Server error: " << e.what() << endl;
        }
    }

    void stop() {
        running = false;
        if (videoThread.joinable()) {
            videoThread.join();
        }
        server.stop();
    }

private:
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

        Detector detector;
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

            ostringstream buf;
            buf << "Mode: " << detector.modeName() << " ||| "
                << "FPS: " << fixed << setprecision(1) << (getTickFrequency() / (double)t);
            putText(frame, buf.str(), Point(10, 30), FONT_HERSHEY_PLAIN, 2.0, Scalar(0, 0, 255), 2, LINE_AA);

            for (auto& r : found) {
                detector.adjustRect(r);
                rectangle(frame, r.tl(), r.br(), Scalar(0, 255, 0), 2);
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

        cap.release();
    }

    Server server;
    set<connection_hdl, owner_less<connection_hdl>> connections;
    mutex connectionsMutex;
    thread videoThread;
    atomic<bool> running;
};

int main(int argc, char** argv) {
    CommandLineParser parser(argc, argv,
        "{ help h   |   | print help message }"
        "{ camera c | 0 | capture video from camera (device index starting from 0) }"
        "{ video v  |   | use video as input }"
        "{ port p   | 5555 | WebSocket server port }");

    parser.about("Face/Body detection with WebSocket streaming capability");

    if (parser.has("help")) {
        parser.printMessage();
        return 0;
    }

    int camera = parser.get<int>("camera");
    string file = parser.get<string>("video");
    int port = parser.get<int>("port");

    if (!parser.check()) {
        parser.printErrors();
        return 1;
    }

    VideoServer server(camera, file);

    try {
        cout << "Video server started on port " << port << endl;
        server.run(port);
    } catch (const exception& e) {
        cerr << "Error: " << e.what() << endl;
    }

    return 0;
}
