import React from 'react';
import { Camera, Activity } from 'lucide-react';

const CameraList = ({ cameras = [], onCameraClick = () => {}, currentCamera = 'camera1' }) => {
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
                    >
                        <Camera className={`w-5 h-5 ${
                            currentCamera === camera.id ? 'animate-pulse' : ''
                        }`} />
                        <div className="flex-1 text-left">
                            <div className="font-medium">{camera.name}</div>
                            <div className="text-sm opacity-80">ID: {camera.id}</div>
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
                    <span>{cameras.length} cameras available</span>
                    <button className="text-blue-500 hover:text-blue-600">
                        Configure
                    </button>
                </div>
            </div>
        </div>
    );
};

export default CameraList;