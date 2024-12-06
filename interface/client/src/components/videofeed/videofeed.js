import React, { useState, useRef, useEffect } from 'react';
import { Camera, Users, Ratio, GalleryHorizontalEnd, Crop, Crosshair, CheckCircle, XCircle } from 'lucide-react';
import { toast } from 'react-toastify';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:4000';

const VideoFeed = ({ cameraId, details, setDetails, wsUrl }) => {
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
    const [frameSize, setFrameSize] = useState({ width: 0, height: 0 });

    // WebSocket Connection Logic
    useEffect(() => {
        let isComponentMounted = true;

        wsRef.current = new WebSocket(wsUrl);
        wsRef.current.binaryType = 'arraybuffer';

        wsRef.current.onopen = () => {
            if (isComponentMounted) {
                console.log('Connected to video stream');
                setIsConnected(true);
            }
        };

        wsRef.current.onclose = () => {
            if (isComponentMounted) {
                console.log('Disconnected from video stream');
                setIsConnected(false);
            }
        };

        wsRef.current.onmessage = (event) => {
            if (!isComponentMounted) return;

            if (typeof event.data === 'string') {
                // Handle JSON data (metrics)
                const data = JSON.parse(event.data);
                setDetails(prev => ({
                    ...prev,
                    peopleCount: data.detectedCount,
                    mode: data.mode,
                    fps: data.fps,
                }));
            } else {
                // Handle binary data (video frame)
                const blob = new Blob([event.data], { type: 'image/jpeg' });
                const imageUrl = URL.createObjectURL(blob);
                const img = new Image();

                img.onload = () => {
                    const canvas = canvasRef.current;
                    if (canvas) {
                        setFrameSize({
                            width: img.naturalWidth,
                            height: img.naturalHeight
                        });

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
        };

        return () => {
            isComponentMounted = false;
            if (wsRef.current) {
                wsRef.current.close();
                wsRef.current = null;
            }
        };
    }, [wsUrl]);

    const convertToRealCoordinates = (canvasX, canvasY) => {
        const rect = canvasRef.current.getBoundingClientRect();
        const scaleX = frameSize.width / rect.width;
        const scaleY = frameSize.height / rect.height;

        return {
            x: Math.round(canvasX * scaleX),
            y: Math.round(canvasY * scaleY)
        };
    };

    // Region Selection Handlers
    const handleCanvasClick = (e) => {
        if (selectionMode === 'start') {
            const rect = canvasRef.current.getBoundingClientRect();
            const canvasX = e.clientX - rect.left;
            const canvasY = e.clientY - rect.top;

            // Convertiamo in coordinate reali
            const realCoords = convertToRealCoordinates(canvasX, canvasY);

            setStartPoint(realCoords);
            setSelectionStart(realCoords);
            setSelectionEnd(realCoords);
            setSelectionMode('region');

            // L'interfaccia visiva continua a usare le coordinate del canvas
            if (markerRef.current) {
                markerRef.current.style.left = `${canvasX}px`;
                markerRef.current.style.top = `${canvasY}px`;
                markerRef.current.style.display = 'block';
            }

            if (selectionRef.current) {
                selectionRef.current.style.left = `${canvasX}px`;
                selectionRef.current.style.top = `${canvasY}px`;
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

        // Calcoliamo le coordinate del canvas, limitate ai bordi
        const canvasX = Math.min(Math.max(0, e.clientX - rect.left), rect.width);
        const canvasY = Math.min(Math.max(0, e.clientY - rect.top), rect.height);

        // Convertiamo in coordinate reali per memorizzazione
        const realCoords = convertToRealCoordinates(canvasX, canvasY);
        setSelectionEnd(realCoords);

        // Calcoliamo le dimensioni per l'interfaccia visiva usando le coordinate del canvas
        const startX = (startPoint.x * rect.width) / frameSize.width; // Convertiamo il punto iniziale reale in coordinate canvas
        const startY = (startPoint.y * rect.height) / frameSize.height;

        const width = Math.abs(canvasX - startX);
        const height = Math.abs(canvasY - startY);
        const left = Math.min(startX, canvasX);
        const top = Math.min(startY, canvasY);

        // Aggiorniamo l'interfaccia visiva
        selectionRef.current.style.left = `${left}px`;
        selectionRef.current.style.top = `${top}px`;
        selectionRef.current.style.width = `${width}px`;
        selectionRef.current.style.height = `${height}px`;
    };

    const handleMouseUp = async () => {
        if (!isDragging || !startPoint) return;
        setIsDragging(false);

        const rect = canvasRef.current.getBoundingClientRect();

        // Converto le coordinate dei punti iniziale e finale in coordinate reali
        const startX = startPoint.x;
        const startY = startPoint.y;
        const endX = selectionEnd.x;
        const endY = selectionEnd.y;

        const width = Math.abs(endX - startX);
        const height = Math.abs(endY - startY);

        // Verifico che la selezione sia abbastanza grande (usando valori reali)
        if (width > 10 && height > 10) {
            const selectedRegion = {
                x: Math.min(startX, endX),
                y: Math.min(startY, endY),
                width,
                height,
                startPoint,
            };
            setSelectedRegion(selectedRegion);
            setSelectionMode('none');

            // Invio i dati al server
            try {
                // Creo l'oggetto con solo i dati necessari
                const windowData = {
                    x: Math.min(startX, endX),
                    y: Math.min(startY, endY),
                    width,
                    height
                };

                await sendWindowData(windowData);

                // Aggiungo un indicatore visuale di successo
                setDetails(prev => ({
                    ...prev,
                    lastWindowUpdate: 'success'
                }));
            } catch (error) {
                // Aggiungo un indicatore visuale di errore
                setDetails(prev => ({
                    ...prev,
                    lastWindowUpdate: 'error'
                }));
            }
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

    const sendWindowData = async (windowData) => {
        try {
            const response = await fetch(`${API_BASE_URL}/window`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(windowData)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const result = await response.json();
            toast.success('Window data sent successfully');
            return result;
        } catch (error) {
            console.error('Error sending window data:', error);
            toast.error('Failed to send window data');
            throw error;
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
                                        <p>Start Point: {selectedRegion.startPoint.x} x {selectedRegion.startPoint.y}</p>
                                        <p>Position: {selectedRegion.x} x {selectedRegion.y}</p>
                                        <p>Size: {selectedRegion.width} x {selectedRegion.height}</p>
                                    </div>
                                </div>
                            </>
                        )}
                    </div>
                )}

                <div className="grid grid-cols-4 gap-4 mt-4">
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

                    <div className="bg-white p-6 rounded-lg shadow border hover:shadow-md transition-shadow">
                        <div className="flex items-center gap-2 text-blue-600">
                            <CheckCircle className="w-5 h-5"/>
                            <h3 className="font-semibold">Services Status</h3>
                        </div>
                        <div className="mt-2 space-y-1">
                            <div className="flex items-center gap-2">
                                {details.serviceStatus.subscribe === 'success' ? (
                                    <CheckCircle className="w-4 h-4 text-green-500" />
                                ) : details.serviceStatus.subscribe === 'failure' ? (
                                    <XCircle className="w-4 h-4 text-red-500" />
                                ) : (
                                    <div className="w-4 h-4 rounded-full border-2 border-gray-300 border-t-transparent animate-spin" />
                                )}
                                <span className="text-sm">Subscribe</span>
                            </div>
                            <div className="flex items-center gap-2">
                                {details.serviceStatus.input === 'success' ? (
                                    <CheckCircle className="w-4 h-4 text-green-500" />
                                ) : details.serviceStatus.input === 'failure' ? (
                                    <XCircle className="w-4 h-4 text-red-500" />
                                ) : (
                                    <div className="w-4 h-4 rounded-full border-2 border-gray-300 border-t-transparent animate-spin" />
                                )}
                                <span className="text-sm">Input</span>
                            </div>
                            <div className="flex items-center gap-2">
                                {details.serviceStatus.config === 'success' ? (
                                    <CheckCircle className="w-4 h-4 text-green-500" />
                                ) : details.serviceStatus.config === 'failure' ? (
                                    <XCircle className="w-4 h-4 text-red-500" />
                                ) : (
                                    <div className="w-4 h-4 rounded-full border-2 border-gray-300 border-t-transparent animate-spin" />
                                )}
                                <span className="text-sm">Config</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default VideoFeed;