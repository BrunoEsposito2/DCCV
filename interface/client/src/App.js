import React, { useState } from 'react';
import './App.css';
import VideoFeed from "./components/videofeed";
import CameraList from "./components/cameralist";

const App = () => {
    const [currentCamera, setCurrentCamera] = useState('camera1');
    const [details, setDetails] = useState({
        peopleCount: 5,
        clothesColor: 'Red, Blue',
    });

    const cameras = [
        { name: 'Camera 1', id: 'camera1' },
        { name: 'Camera 2', id: 'camera2' },
        { name: 'Camera 3', id: 'camera3' },
        // Aggiungi altre videocamere se necessario
    ];

    const handleCameraClick = (cameraId) => {
        setCurrentCamera(cameraId);
        // Aggiorna i dettagli in base alla videocamera selezionata
        setDetails({
            peopleCount: Math.floor(Math.random() * 10),
            clothesColor: 'Color ' + Math.floor(Math.random() * 5),
        });
    };

    return (
        <div className="app-container">
            <VideoFeed cameraId={currentCamera} url={`http://example.com/feed/${currentCamera}`} details={details} />
            <CameraList cameras={cameras} onCameraClick={handleCameraClick} />
        </div>
    );
}

export default App;
