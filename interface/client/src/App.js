import React, { useState, useEffect } from 'react';
import VideoFeed from './components/videofeed';
import CameraList from './components/cameralist';
import './App.css';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:4000';
const VIDEO_WS_URL = process.env.REACT_APP_VIDEO_WS_URL || 'ws://localhost:5555';

const App = () => {
    const [currentCamera, setCurrentCamera] = useState('camera1'); // Valore iniziale di default
    const [details, setDetails] = useState({
        peopleCount: 0,
        mode: 'Initializing...',
        fps: 0,
        serviceStatus: {
            subscribe: 'pending',
            input: 'pending',
            config: 'pending'
        }
    });

    const cameras = [
        { name: 'Main Entrance', id: 'camera1', location: 'Front' },
        { name: 'Parking Area', id: 'camera2', location: 'Exterior' },
        { name: 'Living Room', id: 'camera3', location: 'Indoor' },
    ];

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
                const status = await response.json();
                setDetails(prev => ({
                    ...prev,
                    serviceStatus: {
                        subscribe: status.subscribeStatus,
                        input: status.inputStatus,
                        config: status.configStatus,
                    }
                }));
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
        setCurrentCamera(cameraId); // Update UI immediately

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
        } catch (error) {
            console.error('Error notifying server of camera switch:', error);
            // The UI is already updated, so we don't rollback
            // but you might want to show an error notification
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
                        cameraId={currentCamera}
                        details={details}
                        setDetails={setDetails}
                        wsUrl={`${VIDEO_WS_URL}/${currentCamera}`}
                    />
                </div>
                <div className="md:col-span-1">
                    <CameraList
                        cameras={cameras}
                        onCameraClick={handleCameraClick}
                        currentCamera={currentCamera}
                    />
                </div>
            </div>
        </div>
    );
};

export default App;