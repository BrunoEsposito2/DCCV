import React from 'react';
import { Camera, Activity, RefreshCw } from 'lucide-react';
import { toast } from 'react-toastify';

const CameraList = ({ cameras = [], onCameraClick = () => {}, currentCamera = 'camera1', onConfigureClick = null }) => {
    const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:4000';
    const [configuringCamera, setConfiguringCamera] = React.useState(null);

    // Funzione per gestire il click sul pulsante Configura
    const handleConfigureClick = async (cameraId) => {
        try {
            setConfiguringCamera(cameraId);

            // Invia solo il cameraId al backend usando l'endpoint /window esistente
            const response = await fetch(`${API_BASE_URL}/window`, {
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
            toast.success('Camera configuration request sent successfully');

            // Notify parent component that configure was clicked (for reconnection)
            if (onConfigureClick) {
                onConfigureClick(cameraId);
            }
            setTimeout(() => setConfiguringCamera(null), 5000);
        } catch (error) {
            console.error('Error configuring camera:', error);
            toast.error('Failed to configure camera');
            setConfiguringCamera(null);
        }
    };
    
    if (cameras.length === 0) {
        return (
            <div className="bg-white rounded-lg shadow-lg p-6">
                <div className="mb-6">
                    <h2 className="text-xl font-bold flex items-center gap-2">
                        <Activity className="w-5 h-5" />
                        Camera Controls
                    </h2>
                    <p className="text-sm text-gray-500 mt-1">Loading cameras...</p>
                </div>
                <div className="flex items-center justify-center p-8">
                    <div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                </div>
            </div>
        );
    }

    return (
        <div className="bg-white rounded-lg shadow-lg p-6">
            <div className="mb-6">
                <h2 className="text-xl font-bold flex items-center gap-2">
                    <Activity className="w-5 h-5" />
                    Camera Controls
                </h2>
                <p className="text-sm text-gray-500 mt-1">Select a camera to view its feed</p>
            </div>

            <div className="space-y-3">
                {cameras.map((camera) => (
                    <button
                        key={camera.id}
                        onClick={() => onCameraClick(camera.id)}
                        className={`w-full px-4 py-3 rounded-lg flex items-center gap-3 transition-all duration-200 ${
                            currentCamera === camera.id
                                ? 'bg-blue-500 text-white shadow-lg scale-102'
                                : 'bg-gray-50 text-gray-700 hover:bg-gray-100 hover:scale-102'
                        }`}
                        disabled={configuringCamera === camera.id}
                    >
                        <Camera className={`w-5 h-5 ${
                            currentCamera === camera.id ? 'animate-pulse' : ''
                        }`} />
                        <div className="flex-1 text-left">
                            <div className="font-medium">{camera.name}</div>
                            <div className="text-sm opacity-80">
                                {camera.location ? `Location: ${camera.location}` : `ID: ${camera.id}`}
                            </div>
                        </div>
                        {currentCamera === camera.id && (
                            <span className="text-xs bg-blue-600 px-2 py-1 rounded-full">
                                Active
                              </span>
                        )}
                    </button>
                ))}
            </div>

            <div className="mt-6 pt-4 border-t">
                <div className="text-sm text-gray-500 flex justify-between items-center">
                    <span>{cameras.length} {cameras.length === 1 ? 'camera' : 'cameras'} available</span>
                    <button 
                        className={`flex items-center gap-2 px-3 py-1 rounded transition-colors ${
                            configuringCamera === currentCamera
                                ? 'bg-gray-200 text-gray-500 cursor-not-allowed'
                                : 'text-blue-500 hover:text-blue-600 hover:bg-blue-50'
                        }`}
                        onClick={() => handleConfigureClick(currentCamera)}
                        disabled={configuringCamera === currentCamera}
                    >
                        {configuringCamera === currentCamera ? (
                            <>
                                <RefreshCw className="w-4 h-4 animate-spin" />
                                Restarting...
                            </>
                        ) : (
                            <>
                                <RefreshCw className="w-4 h-4" />
                                Restart Camera
                            </>
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default CameraList;