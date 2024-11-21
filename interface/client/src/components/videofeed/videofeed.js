import React, { useState, useRef, useEffect } from 'react';
import { Camera, Users, Palette, Maximize2, Download, AlertCircle, Crop, Crosshair } from 'lucide-react';
import * as Dialog from '@radix-ui/react-dialog';
import * as AlertDialog from '@radix-ui/react-alert-dialog';

const VideoFeed = ({ cameraId = '1', details = { peopleCount: 0, clothesColor: 'None' } }) => {
    const canvasRef = useRef(null);
    const selectionRef = useRef(null);
    const markerRef = useRef(null);
    const [isConnected, setIsConnected] = useState(true);
    const [showDisconnectAlert, setShowDisconnectAlert] = useState(false);
    const [selectionMode, setSelectionMode] = useState('none'); // 'none', 'start', 'region'
    const [startPoint, setStartPoint] = useState(null);
    const [selectionStart, setSelectionStart] = useState(null);
    const [selectionEnd, setSelectionEnd] = useState(null);
    const [selectedRegion, setSelectedRegion] = useState(null);
    const [isDragging, setIsDragging] = useState(false);

    useEffect(() => {
        const canvas = canvasRef.current;
        if (canvas) {
            const ctx = canvas.getContext('2d');
            canvas.width = 640;
            canvas.height = 480;
            ctx.fillStyle = '#2D3748';
            ctx.fillRect(0, 0, canvas.width, canvas.height);
            ctx.fillStyle = '#A0AEC0';
            ctx.font = '20px Arial';
            ctx.textAlign = 'center';
            ctx.fillText(`Camera Feed ${cameraId}`, canvas.width / 2, canvas.height / 2);
        }
    }, [cameraId]);

    const handleCanvasClick = (e) => {
        if (selectionMode === 'start') {
            const rect = canvasRef.current.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;

            const percentageX = (x / rect.width) * 100;
            const percentageY = (y / rect.height) * 100;

            setStartPoint({ x: percentageX, y: percentageY });
            setSelectionStart({ x, y });
            setSelectionEnd({ x, y });
            setSelectionMode('region');

            // Update marker position
            if (markerRef.current) {
                markerRef.current.style.left = `${x}px`;
                markerRef.current.style.top = `${y}px`;
                markerRef.current.style.display = 'block';
            }

            // Initialize selection box and start dragging
            if (selectionRef.current) {
                selectionRef.current.style.left = `${x}px`;
                selectionRef.current.style.top = `${y}px`;
                selectionRef.current.style.width = '0px';
                selectionRef.current.style.height = '0px';
                selectionRef.current.style.display = 'block';
            }

            // Automatically start dragging
            setIsDragging(true);
        }
    };

    const handleMouseDown = (e) => {
        if (selectionMode === 'region' && startPoint && !isDragging) {
            const rect = canvasRef.current.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;

            // Only start dragging if click is near the start point
            const startX = (startPoint.x * rect.width) / 100;
            const startY = (startPoint.y * rect.height) / 100;
            const distance = Math.sqrt(Math.pow(x - startX, 2) + Math.pow(y - startY, 2));

            if (distance < 20) { // 20px threshold for starting drag
                setIsDragging(true);
                setSelectionEnd({ x, y });
            }
        }
    };

    const handleMouseMove = (e) => {
        if (!isDragging || !selectionRef.current || !startPoint) return;

        const rect = canvasRef.current.getBoundingClientRect();
        const x = Math.min(Math.max(0, e.clientX - rect.left), rect.width);
        const y = Math.min(Math.max(0, e.clientY - rect.top), rect.height);
        setSelectionEnd({ x, y });

        // Calculate dimensions based on start point and current position
        const startX = (startPoint.x * rect.width) / 100;
        const startY = (startPoint.y * rect.height) / 100;
        const width = Math.abs(x - startX);
        const height = Math.abs(y - startY);
        const left = Math.min(startX, x);
        const top = Math.min(startY, y);

        // Update selection box
        selectionRef.current.style.left = `${left}px`;
        selectionRef.current.style.top = `${top}px`;
        selectionRef.current.style.width = `${width}px`;
        selectionRef.current.style.height = `${height}px`;
    };

    const handleMouseUp = () => {
        if (!isDragging || !startPoint) return;
        setIsDragging(false);

        const rect = canvasRef.current.getBoundingClientRect();
        const startX = (startPoint.x * rect.width) / 100;
        const startY = (startPoint.y * rect.height) / 100;
        const endX = selectionEnd.x;
        const endY = selectionEnd.y;

        const width = Math.abs(endX - startX);
        const height = Math.abs(endY - startY);

        if (width > 10 && height > 10) {
            const selectedRegion = {
                x: Math.min(startX, endX),
                y: Math.min(startY, endY),
                width,
                height,
                startPoint,
                percentages: {
                    x: (Math.min(startX, endX) / rect.width) * 100,
                    y: (Math.min(startY, endY) / rect.height) * 100,
                    width: (width / rect.width) * 100,
                    height: (height / rect.height) * 100
                }
            };
            setSelectedRegion(selectedRegion);
            console.log('Selected region with start point:', selectedRegion);
            setSelectionMode('none');
        }
    };

    const resetSelection = () => {
        setSelectionMode('none');
        setStartPoint(null);
        setSelectedRegion(null);
        if (markerRef.current) {
            markerRef.current.style.display = 'none';
        }
        if (selectionRef.current) {
            selectionRef.current.style.display = 'none';
        }
    };

    return (
        <div className="bg-white rounded-lg shadow-lg p-6">
            <div className="mb-4">
                <div className="flex justify-between items-center">
                    <h2 className="text-2xl font-bold flex items-center gap-2">
                        <Camera className="w-6 h-6" />
                        Camera Feed {cameraId}
                    </h2>
                    <div className="flex gap-2">
                        <button
                            onClick={() => {
                                if (selectionMode === 'none') {
                                    setSelectionMode('start');
                                } else {
                                    resetSelection();
                                }
                            }}
                            className={`flex items-center gap-2 px-3 py-1 rounded-full transition-colors ${
                                selectionMode !== 'none'
                                    ? 'bg-blue-500 text-white'
                                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                            }`}
                        >
                            {selectionMode === 'start' ? (
                                <>
                                    <Crosshair className="w-4 h-4" />
                                    Select Start Point
                                </>
                            ) : selectionMode === 'region' ? (
                                <>
                                    <Crop className="w-4 h-4" />
                                    Select Region
                                </>
                            ) : (
                                <>
                                    <Crosshair className="w-4 h-4" />
                                    Start Selection
                                </>
                            )}
                        </button>
                        {isConnected ? (
                            <div className="flex items-center gap-2 text-green-500 bg-green-50 px-3 py-1 rounded-full">
                                <span className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></span>
                                Live
                            </div>
                        ) : (
                            /* Rest of connection status UI */
                            null
                        )}
                    </div>
                </div>
            </div>

            <div className="space-y-4">
                <div className="relative group">
                    <Dialog.Root>
                        <div className="border rounded-lg overflow-hidden bg-gray-900 relative">
                            <canvas
                                ref={canvasRef}
                                className={`w-full ${
                                    selectionMode === 'start' ? 'cursor-crosshair' :
                                        selectionMode === 'region' ? 'cursor-crosshair' :
                                            'cursor-pointer'
                                }`}
                                style={{ maxWidth: '100%', height: 'auto', display: 'block' }}
                                onClick={handleCanvasClick}
                                onMouseDown={handleMouseDown}
                                onMouseMove={handleMouseMove}
                                onMouseUp={handleMouseUp}
                                onMouseLeave={handleMouseUp}
                            />
                            <div
                                ref={markerRef}
                                className="absolute w-4 h-4 -translate-x-1/2 -translate-y-1/2 hidden pointer-events-none"
                                style={{
                                    backgroundImage: `radial-gradient(circle, rgba(59, 130, 246, 0.5) 0%, rgba(59, 130, 246, 0.2) 50%, transparent 70%)`,
                                    width: '20px',
                                    height: '20px',
                                    borderRadius: '50%'
                                }}
                            />
                            <div
                                ref={selectionRef}
                                className="absolute border-2 border-blue-500 bg-blue-500/20 hidden pointer-events-none"
                            />
                        </div>

                        {/* Rest of Dialog components */}
                    </Dialog.Root>
                </div>

                {(startPoint || selectedRegion) && (
                    <div className="bg-blue-50 p-4 rounded-lg">
                        {startPoint && !selectedRegion && (
                            <>
                                <h3 className="font-semibold text-blue-700 mb-2">Selected Start Point</h3>
                                <div className="text-sm">
                                    <p>Position: {startPoint.x.toFixed(1)}% x {startPoint.y.toFixed(1)}%</p>
                                </div>
                            </>
                        )}
                        {selectedRegion && (
                            <>
                                <h3 className="font-semibold text-blue-700 mb-2">Selected Region</h3>
                                <div className="grid grid-cols-2 gap-4 text-sm">
                                    <div>
                                        <p>Start: {selectedRegion.startPoint.x.toFixed(1)}% x {selectedRegion.startPoint.y.toFixed(1)}%</p>
                                        <p>Position: {selectedRegion.percentages.x.toFixed(1)}% x {selectedRegion.percentages.y.toFixed(1)}%</p>
                                        <p>Size: {selectedRegion.percentages.width.toFixed(1)}% x {selectedRegion.percentages.height.toFixed(1)}%</p>
                                    </div>
                                </div>
                            </>
                        )}
                    </div>
                )}

                <div className="grid grid-cols-2 gap-4">
                    <div className="bg-white p-6 rounded-lg shadow border hover:shadow-md transition-shadow">
                        <div className="flex items-center gap-2 text-blue-600">
                            <Users className="w-5 h-5" />
                            <h3 className="font-semibold">People Count</h3>
                        </div>
                        <p className="text-3xl font-bold mt-2">{details.peopleCount}</p>
                        <p className="text-sm text-gray-500 mt-1">Current occupancy</p>
                    </div>

                    <div className="bg-white p-6 rounded-lg shadow border hover:shadow-md transition-shadow">
                        <div className="flex items-center gap-2 text-purple-600">
                            <Palette className="w-5 h-5" />
                            <h3 className="font-semibold">Detected Colors</h3>
                        </div>
                        <p className="text-3xl font-bold mt-2">{details.clothesColor}</p>
                        <p className="text-sm text-gray-500 mt-1">Predominant colors</p>
                    </div>
                </div>
            </div>

            <AlertDialog.Root open={showDisconnectAlert}>
                <AlertDialog.Portal>
                    <AlertDialog.Overlay className="fixed inset-0 bg-black/50" />
                    <AlertDialog.Content className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-white p-6 rounded-lg shadow-xl w-full max-w-md">
                        <AlertDialog.Title className="text-xl font-bold mb-4">Connection Lost</AlertDialog.Title>
                        <AlertDialog.Description className="text-gray-600 mb-6">
                            The connection to Camera {cameraId} has been lost. Would you like to attempt to reconnect?
                        </AlertDialog.Description>
                        <div className="flex justify-end gap-4">
                            <AlertDialog.Cancel asChild>
                                <button className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200">
                                    Cancel
                                </button>
                            </AlertDialog.Cancel>
                            <AlertDialog.Action asChild>
                                <button onClick={() => setIsConnected(true)} className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600">
                                    Reconnect
                                </button>
                            </AlertDialog.Action>
                        </div>
                    </AlertDialog.Content>
                </AlertDialog.Portal>
            </AlertDialog.Root>
        </div>
    );
};

export default VideoFeed;