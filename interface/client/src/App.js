import React, { useState } from 'react';
import VideoFeed from './components/videofeed';
import CameraList from './components/cameralist';
import './App.css';

const App = () => {
    const [currentCamera, setCurrentCamera] = useState('camera1');
    const [details, setDetails] = useState({
        peopleCount: 5,
        clothesColor: 'Red, Blue',
    });

    const cameras = [
        { name: 'Main Entrance', id: 'camera1', location: 'Front' },
        { name: 'Parking Area', id: 'camera2', location: 'Exterior' },
        { name: 'Back Door', id: 'camera3', location: 'Rear' },
    ];

    const handleCameraClick = (cameraId) => {
        setCurrentCamera(cameraId);
        setDetails({
            peopleCount: Math.floor(Math.random() * 10),
            clothesColor: 'Color ' + Math.floor(Math.random() * 5),
        });
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
                    <VideoFeed cameraId={currentCamera} details={details} />
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