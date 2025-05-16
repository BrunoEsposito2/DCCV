import React, { useState, useEffect, useRef } from 'react';
import VideoFeed from './components/videofeed';
import CameraList from './components/cameralist';
import './App.css';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:4000';
const VIDEO_WS_URL = process.env.REACT_APP_VIDEO_WS_URL || 'ws://localhost:5555';

const App = () => {
    const videoFeedRef = useRef(null);
    const [currentCamera, setCurrentCamera] = useState('camera1'); // Valore iniziale di default
    const [cameras, setCameras] = useState([]);
    const [details, setDetails] = useState({
        peopleCount: 0,
        mode: 'Initializing...',
        fps: 0,
        serviceStatus: {
            subscribe: 'pending',
            input: 'pending',
            config: 'pending'
        },
        lastWindowUpdate: null
    });

    const [reconnectKey, setReconnectKey] = useState(0); // Used to force VideoFeed to reconnect

    // Handle configure click from CameraList
    const handleConfigureClick = (cameraId) => {
        console.log(`Configure clicked for camera: ${cameraId}`);

        try {
            // Usa un ID specifico per il toast e controlla se esiste prima di rimuoverlo
            toast.dismiss('camera-restart');
            
            // Mostra un nuovo toast con ID specifico
            toast.info('Restarting camera...', {
                toastId: 'camera-restart',
                autoClose: 3000
            });
    
            // Aggiungi un breve ritardo prima di cambiare il reconnectKey
            setTimeout(() => {
                setReconnectKey(prevKey => prevKey + 1);
            }, 600); // Un breve ritardo per permettere al toast di essere visualizzato
        } catch (error) {
            console.error('Error handling toast during camera restart:', error);
            // Forza comunque il reconnect anche se ci sono errori con i toast
            setTimeout(() => {
                setReconnectKey(prevKey => prevKey + 1);
            }, 2000);
        }
    };

    // Polling dello stato dei servizi
    useEffect(() => {
        const pollStatus = async () => {
            try {
                const response = await fetch(`${API_BASE_URL}/status`);
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                const contentType = response.headers.get("content-type");
                if (!contentType || !contentType.includes("application/json")) {
                    throw new TypeError("Response is not JSON");
                }
                const data = await response.json();
                setDetails(prev => ({
                    ...prev,
                    serviceStatus: {
                        subscribe: data.subscribeStatus,
                        input: data.inputStatus,
                        config: data.configStatus
                    },
                    peopleCount: data.peopleCount || prev.peopleCount,
                    mode: data.mode || prev.mode,
                    fps: data.fps || prev.fps
                }));

                if (data.cameras && Array.isArray(data.cameras)) {
                    setCameras(data.cameras);

                    // Se abbiamo ricevuto l'informazione sulla camera corrente dal server
                    if (data.currentCamera) {
                        setCurrentCamera(data.currentCamera);
                    }
                }
            } catch (error) {
                console.error('Error polling status:', error);
                // Optionally set an error state or show a notification
            }
        };

        const interval = setInterval(pollStatus, 5000);
        pollStatus(); // Prima chiamata immediata

        return () => clearInterval(interval);
    }, []);

    const handleCameraClick = async (cameraId) => {
        // Se è già la camera corrente, non fare nulla
        //if (cameraId === currentCamera) return;

        setCurrentCamera(cameraId); // Update UI immediately

        // Dismiss any existing toasts
        if (toast && toast.dismiss) {
            toast.dismiss();
        }
        
        // Show info toast
        toast.info(`Switching to ${cameraId}...`, {
            toastId: 'camera-switch',
            autoClose: 2000
        });

        try {
            const response = await fetch(`${API_BASE_URL}/camera/switch`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ cameraId }),
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            // Optional: verify the response data if needed
            console.log('Camera switch successful:', data);

            // Force reconnection when switching cameras
            setReconnectKey(prevKey => prevKey + 1);

        } catch (error) {
            console.error('Error notifying server of camera switch:', error);
            toast.error('Failed to switch camera', {
                toastId: 'camera-switch-error'
            });
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 p-4 md:p-8">
            <header className="mb-8">
                <h1 className="text-3xl font-bold text-gray-900">
                    Video Surveillance Dashboard
                </h1>
                <p className="text-gray-600 mt-2">
                    Real-time monitoring and analysis system
                </p>
            </header>

            <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                <div className="md:col-span-3">
                    <VideoFeed
                        key={`videofeed-${currentCamera}-${reconnectKey}`} // Force re-mount when these change
                        ref={videoFeedRef}
                        cameraId={currentCamera}
                        details={details}
                        setDetails={setDetails}
                        wsUrl={`${VIDEO_WS_URL}`} // Base URL, component will form the full path
                    />
                </div>
                <div className="md:col-span-1">
                    <CameraList
                        cameras={cameras}
                        onCameraClick={handleCameraClick}
                        currentCamera={currentCamera}
                        onConfigureClick={handleConfigureClick}
                    />
                </div>
            </div>
            <ToastContainer 
                position="bottom-right"
                autoClose={5000}
                hideProgressBar={false}
                newestOnTop
                closeOnClick
                rtl={false}
                pauseOnFocusLoss
                draggable
                pauseOnHover
                limit={3} // Limita il numero di notifiche visualizzate contemporaneamente
                theme="light"
            />
        </div>
    );
};

export default App;