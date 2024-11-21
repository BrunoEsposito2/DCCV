import React, { useState, useRef, useEffect } from 'react';
import { Camera, Users, Ratio, GalleryHorizontalEnd, Crop, Crosshair } from 'lucide-react';
import * as AlertDialog from '@radix-ui/react-alert-dialog';

const VideoFeed = ({ cameraId, details, setDetails, wsEndpoint }) => {
    const canvasRef = useRef(null);
    const selectionRef = useRef(null);
    const markerRef = useRef(null);
    const wsRef = useRef(null);

    const [isConnected, setIsConnected] = useState(false);
    const [selectionMode, setSelectionMode] = useState('none'); // 'none', 'start', 'region'
    const [startPoint, setStartPoint] = useState(null);
    const [selectionStart, setSelectionStart] = useState(null);
    const [selectionEnd, setSelectionEnd] = useState(null);
    const [selectedRegion, setSelectedRegion] = useState(null);
    const [isDragging, setIsDragging] = useState(false);

    // WebSocket Connection Logic
    useEffect(() => {
        let reconnectTimer;

        const connectWebSocket = () => {
            try {
                wsRef.current = new WebSocket(wsEndpoint);
                wsRef.current.binaryType = 'arraybuffer';

                wsRef.current.onopen = () => {
                    console.log('Connected to video server');
                    setIsConnected(true);
                    if (reconnectTimer) clearTimeout(reconnectTimer);
                };

                wsRef.current.onclose = () => {
                    console.log('Disconnected from video server');
                    setIsConnected(false);
                    reconnectTimer = setTimeout(connectWebSocket, 3000);
                };

                wsRef.current.onerror = (error) => {
                    console.error('WebSocket error:', error);
                    setIsConnected(false);
                };

                wsRef.current.onmessage = (event) => {
                    try {
                        if (typeof event.data === 'string') {
                            // Handle JSON data (metrics)
                            const data = JSON.parse(event.data);
                            setDetails({
                                peopleCount: data.detectedCount,
                                mode: data.mode,
                                fps: data.fps
                            });
                        } else {
                            // Handle binary data (video frame)
                            const blob = new Blob([event.data], { type: 'image/jpeg' });
                            const imageUrl = URL.createObjectURL(blob);
                            const img = new Image();

                            img.onload = () => {
                                const canvas = canvasRef.current;
                                if (canvas) {
                                    const ctx = canvas.getContext('2d');
                                    if (canvas.width !== img.width) {
                                        canvas.width = img.width;
                                        canvas.height = img.height;
                                    }
                                    ctx.drawImage(img, 0, 0);
                                    URL.revokeObjectURL(imageUrl);
                                }
                            };
                            img.src = imageUrl;
                        }
                    } catch (error) {
                        console.error('Error processing message:', error);
                    }
                };

            } catch (error) {
                console.error('WebSocket connection error:', error);
                setIsConnected(false);
                reconnectTimer = setTimeout(connectWebSocket, 3000);
            }
        };

        connectWebSocket();

        return () => {
            if (wsRef.current) wsRef.current.close();
            if (reconnectTimer) clearTimeout(reconnectTimer);
        };
    }, [wsEndpoint, setDetails]);

    // Region Selection Handlers
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

            if (markerRef.current) {
                markerRef.current.style.left = `${x}px`;
                markerRef.current.style.top = `${y}px`;
                markerRef.current.style.display = 'block';
            }

            if (selectionRef.current) {
                selectionRef.current.style.left = `${x}px`;
                selectionRef.current.style.top = `${y}px`;
                selectionRef.current.style.width = '0px';
                selectionRef.current.style.height = '0px';
                selectionRef.current.style.display = 'block';
            }

            setIsDragging(true);
        }
    };

    const handleMouseDown = (e) => {
        if (selectionMode === 'region' && startPoint && !isDragging) {
            const rect = canvasRef.current.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;

            const startX = (startPoint.x * rect.width) / 100;
            const startY = (startPoint.y * rect.height) / 100;
            const distance = Math.sqrt(Math.pow(x - startX, 2) + Math.pow(y - startY, 2));

            if (distance < 20) {
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

        const startX = (startPoint.x * rect.width) / 100;
        const startY = (startPoint.y * rect.height) / 100;
        const width = Math.abs(x - startX);
        const height = Math.abs(y - startY);
        const left = Math.min(startX, x);
        const top = Math.min(startY, y);

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
                        ) : null}
                    </div>
                </div>
            </div>

            <div className="space-y-4">
                <div className="relative group">
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

                <div className="grid grid-cols-3 gap-4">
                    <div className="bg-white p-6 rounded-lg shadow border hover:shadow-md transition-shadow">
                        <div className="flex items-center gap-2 text-blue-600">
                            <Users className="w-5 h-5"/>
                            <h3 className="font-semibold">People Count</h3>
                        </div>
                        <p className="text-3xl font-bold mt-2">{details.peopleCount}</p>
                        <p className="text-sm text-gray-500 mt-1">Current occupancy</p>
                    </div>

                    <div className="bg-white p-6 rounded-lg shadow border hover:shadow-md transition-shadow">
                        <div className="flex items-center gap-2 text-blue-600">
                            <Ratio className="w-5 h-5"/>
                            <h3 className="font-semibold">Mode</h3>
                        </div>
                        <p className="text-3xl font-bold mt-2">{details.mode}</p>
                        <p className="text-sm text-gray-500 mt-1">Current tracking mode</p>
                    </div>

                    <div className="bg-white p-6 rounded-lg shadow border hover:shadow-md transition-shadow">
                        <div className="flex items-center gap-2 text-blue-600">
                            <GalleryHorizontalEnd className="w-5 h-5"/>
                            <h3 className="font-semibold">Frame per second</h3>
                        </div>
                        <p className="text-3xl font-bold mt-2">{details.fps}</p>
                        <p className="text-sm text-gray-500 mt-1">Video stream fps</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default VideoFeed;