import React, { useState, useRef, useEffect } from 'react';
import { Camera, Users, Ratio, GalleryHorizontalEnd, Crop, Crosshair } from 'lucide-react';
import { toast } from 'react-toastify';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:4000';

const VideoFeed = ({ cameraId, details, setDetails, wsUrl }) => {
    const canvasRef = useRef(null);
    const selectionRef = useRef(null);
    const markerRef = useRef(null);
    const wsRef = useRef(null);

    const reconnectTimeoutRef = useRef(null);
    const connectionStabilized = useRef(false);
    const toastShownRef = useRef(false);
    
    const reconnectAttemptsRef = useRef(0);

    const [isConnected, setIsConnected] = useState(false);
    const [isConnecting, setIsConnecting] = useState(false);
    const [selectionMode, setSelectionMode] = useState('none'); // 'none', 'start', 'region'
    const [startPoint, setStartPoint] = useState(null);
    const [selectionStart, setSelectionStart] = useState(null);
    const [selectionEnd, setSelectionEnd] = useState(null);
    const [selectedRegion, setSelectedRegion] = useState(null);
    const [isDragging, setIsDragging] = useState(false);
    const [frameSize, setFrameSize] = useState({ width: 0, height: 0 });
    const [lastToastTime, setLastToastTime] = useState(0);

    const [reconnectAttempts, setReconnectAttempts] = useState(0);
    const MAX_RECONNECT_ATTEMPTS = 10; // Try reconnecting up to 10 times
    const RECONNECT_DELAY_BASE = 1000;
    const TOAST_COOLDOWN = 10000; // 10 seconds between connection toasts

    const lastFrameTimeRef = useRef(Date.now());
    const reconnectDelayTimerRef = useRef(null);
    const [showReconnecting, setShowReconnecting] = useState(false);
    const RECONNECT_DISPLAY_DELAY = 3000; // Ritardo di 3 secondi prima di mostrare l'overlay

    // Function to show toast with frequency limiting
    const showLimitedToast = (message, type = 'success') => {
        const now = Date.now();
        if (now - lastToastTime > TOAST_COOLDOWN) {
            setLastToastTime(now);
            if (type === 'success') {
                toast.success(message, { toastId: 'connection-toast' });
            } else if (type === 'error') {
                toast.error(message, { toastId: 'connection-toast' });
            } else if (type === 'info') {
                toast.info(message, { toastId: 'connection-toast' });
            }
        }
    };

    // Function to connect to WebSocket
    const connectToWebSocket = () => {
        if (isConnecting) return;
        setIsConnecting(true);

        // Close existing connection if any
        if (wsRef.current && wsRef.current.readyState !== WebSocket.CLOSED) {
            wsRef.current.close();
        }

        // Make sure we have the correct WebSocket URL format
        // Handle both formats: "camera1" or just "1"
        const formattedId = cameraId.startsWith('camera') ? cameraId : `camera${cameraId}`;
        // Make sure URL ends with /camera{id} not /{id}
        const correctWsUrl = `${wsUrl}/camera${formattedId.replace('camera', '')}`;
        
        console.log(`Attempting to connect to WebSocket at: ${correctWsUrl}`);
        
        // Create new WebSocket
        wsRef.current = new WebSocket(correctWsUrl);
        wsRef.current.binaryType = 'arraybuffer';

        wsRef.current.onopen = () => {
            console.log('Connected to video stream');
            setIsConnected(true);
            setIsConnecting(false);
            reconnectAttemptsRef.current = 0;
            setReconnectAttempts(0);

            // Only show toast if this is the first successful connection after a restart
            if (!connectionStabilized.current && !toastShownRef.current) {
                showLimitedToast('Video stream connected');
                toastShownRef.current = true;
                
                // After first successful connection, mark as stabilized after a delay
                setTimeout(() => {
                    connectionStabilized.current = true;
                }, 5000);
            }
        };

        wsRef.current.onclose = (event) => {
            console.log(`WebSocket closed: ${event.code} ${event.reason}`);
            
            setIsConnected(false);
            setIsConnecting(false);

            // Cancella qualsiasi timer precedente
            if (reconnectDelayTimerRef.current) {
                clearTimeout(reconnectDelayTimerRef.current);
            }

            // Programma un timer che mostrerà l'overlay solo se il ritardo è significativo
            reconnectDelayTimerRef.current = setTimeout(() => {
                // Controlla il tempo trascorso dall'ultimo frame
                const timeSinceLastFrame = Date.now() - lastFrameTimeRef.current;
                
                // Se sono trascorsi più di X secondi dall'ultimo frame, mostra l'overlay
                if (timeSinceLastFrame > RECONNECT_DISPLAY_DELAY) {
                    setShowReconnecting(true);
                }
                
                reconnectDelayTimerRef.current = null;
            }, RECONNECT_DISPLAY_DELAY);
            
            if (isConnected && connectionStabilized.current) {
                // Try to reconnect if not at max attempts
                if (reconnectAttemptsRef.current < MAX_RECONNECT_ATTEMPTS) {
                    reconnectAttemptsRef.current = reconnectAttemptsRef.current + 1;
                    setReconnectAttempts(reconnectAttemptsRef.current);
                    
                    console.log(`WebSocket reconnecting (attempt ${reconnectAttemptsRef.current}/${MAX_RECONNECT_ATTEMPTS})`);
                    
                    // Exponential backoff: wait longer between attempts
                    const delay = Math.min(RECONNECT_DELAY_BASE * Math.pow(1.5, reconnectAttemptsRef.current), 10000);
                    
                    if (reconnectTimeoutRef.current) {
                        clearTimeout(reconnectTimeoutRef.current);
                    }

                    reconnectTimeoutRef.current = setTimeout(() => {
                        //setIsConnecting(false); // Ensure we can connect again
                        connectToWebSocket();
                    }, delay);    
                } else {
                    console.log('Max reconnection attempts reached');
                    showLimitedToast('Failed to connect to video stream. Please try refreshing the page.', 'error');
                }
            } else {
                // Simple retry with fixed delay for initial connection
                if (reconnectAttemptsRef.current < MAX_RECONNECT_ATTEMPTS) {
                    reconnectAttemptsRef.current = reconnectAttemptsRef.current + 1;
                    setReconnectAttempts(reconnectAttemptsRef.current);
                    
                    // Clear any existing reconnect timeout
                    if (reconnectTimeoutRef.current) {
                        clearTimeout(reconnectTimeoutRef.current);
                    }
                    
                    reconnectTimeoutRef.current = setTimeout(() => {
                        //setIsConnecting(false);
                        connectToWebSocket();
                    }, 2000); // Fixed 2-second delay for initial connection
                }
            }
            
            
        };

        wsRef.current.onerror = (error) => {
            console.error('WebSocket error:', error);
            setIsConnecting(false);
        };

        wsRef.current.onmessage = (event) => {
            lastFrameTimeRef.current = Date.now();

            setIsConnected(true)
            setIsConnecting(false)

            // Se stava mostrando l'overlay di riconnessione, nascondilo immediatamente
            if (showReconnecting) {
                setShowReconnecting(false);
            }

            // Cancella qualsiasi timer di reconnecting display
            if (reconnectDelayTimerRef.current) {
                clearTimeout(reconnectDelayTimerRef.current);
                reconnectDelayTimerRef.current = null;
            }

            // Reset del contatore di tentativi quando riceviamo frame
            if (reconnectAttemptsRef.current > 0) {
                reconnectAttemptsRef.current = 0;
                setReconnectAttempts(0);
            }

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
                    ctx.clearRect(0, 0, canvas.width, canvas.height);
                    ctx.drawImage(img, 0, 0);
                    URL.revokeObjectURL(imageUrl);
                }
            };
            img.src = imageUrl;
        };
    };

    // Reset connection stabilization when camera changes
    useEffect(() => {
        connectionStabilized.current = false;
        toastShownRef.current = false;

        setIsConnected(false);

        reconnectAttemptsRef.current = 0;
        setReconnectAttempts(0);
        // Clear any toast notifications
        if (toast && toast.dismiss) {
            toast.dismiss();
        }
    }, [cameraId]);

    // WebSocket Connection Logic with delay
    useEffect(() => {
        console.log(`Camera ID changed to: ${cameraId}`);

        const initDelay = setTimeout(() => {
            connectToWebSocket();
        }, 3000);
        
        return () => {
            clearTimeout(initDelay);
            if (reconnectTimeoutRef.current) {
                clearTimeout(reconnectTimeoutRef.current);
            }
            if (reconnectDelayTimerRef.current) {
                clearTimeout(reconnectDelayTimerRef.current);
            }
            if (wsRef.current) {
                try {
                    if (wsRef.current.readyState !== WebSocket.CLOSED) {
                        wsRef.current.close();
                    }
                } catch (e) {
                    console.error('Error closing WebSocket:', e);
                }
                wsRef.current = null;
            }
            if (toast && toast.dismiss) {
                try {
                    toast.dismiss();
                } catch (e) {
                    console.error('Error dismissing toasts:', e);
                }
                
            }
        }; 
    }, [cameraId, wsUrl]);

    const convertToRealCoordinates = (canvasX, canvasY) => {
        const rect = canvasRef.current.getBoundingClientRect();

        const realWidth = frameSize.width || canvasRef.current.width;
        const realHeight = frameSize.height || canvasRef.current.height;

        const scaleX = realWidth / rect.width;
        const scaleY = realHeight / rect.height;

        return {
            x: Math.max(0, Math.round(canvasX * scaleX)),
            y: Math.max(0, Math.round(canvasY * scaleY))
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

            // Reset connection stabilization flag before restart
            connectionStabilized.current = false;
            toastShownRef.current = false;

            // Invio i dati al server
            try {
                // Creo l'oggetto con solo i dati necessari
                const windowData = {
                    x: Math.min(startX, endX),
                    y: Math.min(startY, endY),
                    width,
                    height
                };

                // Clear existing toasts before showing new ones
                if (toast && toast.dismiss) {
                    toast.dismiss();
                }

                await sendWindowData(windowData);

                // Aggiungo un indicatore visuale di successo
                setDetails(prev => ({
                    ...prev,
                    lastWindowUpdate: 'success'
                }));

                // Close current connection before attempting to reconnect
                if (wsRef.current && wsRef.current.readyState !== WebSocket.CLOSED) {
                    wsRef.current.close();
                    wsRef.current = null;
                }
                
                // Reset reconnect attempts when we're initiating a new connection
                setReconnectAttempts(0);
                
                // Wait for backend to restart the C++ process
                toast.info("Restarting video stream with new window settings...", { 
                    toastId: 'restart-toast',
                    autoClose: 5000
                });
                
                // When we configure a new window, try reconnecting to the WebSocket
                // after a short delay to give the C++ application time to restart
                setTimeout(connectToWebSocket, 5000);
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
            const factorX = 2.0; // Il backend ridimensiona a 0.5, quindi dobbiamo moltiplicare per 2
            const factorY = 2.0;
            
            const adjustedData = {
                x: Math.max(0, Math.round(windowData.x * factorX)),
                y: Math.max(0, Math.round(windowData.y * factorY)),
                width: Math.max(10, Math.round(windowData.width * factorX)),
                height: Math.max(10, Math.round(windowData.height * factorY))
            };

            const response = await fetch(`${API_BASE_URL}/window`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(adjustedData)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const result = await response.json();
            toast.success('Window data sent successfully', { 
                toastId: 'window-data-toast',
                autoClose: 3000
            });

            return result;
        } catch (error) {
            console.error('Error sending window data:', error);
            toast.error('Failed to send window data', { 
                toastId: 'window-data-error-toast' 
            });
            throw error;
        }
    };

    const handleReconnect = () => {
        // Don't reconnect if already connecting
        if (isConnecting) return;

        // Close the current connection if it exists
        if (wsRef.current) {
            wsRef.current.close();
            wsRef.current = null;
        }
        
        // Reset the reconnect attempts
        reconnectAttemptsRef.current = 0;
        setReconnectAttempts(0);
        setIsConnected(false);

        setIsConnecting(false);

        // Reset connection stabilization flag
        connectionStabilized.current = false;
        toastShownRef.current = false;

        // Clear existing toasts
        if (toast && toast.dismiss) {
            toast.dismiss();
        }

        // Show reconnecting toast
        toast.info('Reconnecting to camera feed...', { 
            toastId: 'reconnect-toast',
            autoClose: 3000
        });
        
        // Try to connect again
        setTimeout(() => {
            connectToWebSocket();
        }, 1000);
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
                            <div className="flex items-center gap-2 text-yellow-500 bg-yellow-50 px-3 py-1 rounded-full">
                                <span className="w-2 h-2 bg-yellow-500 rounded-full animate-pulse"></span>
                                Connecting{reconnectAttempts > 0 ? ` (${reconnectAttempts})` : ''}
                            </div>
                        )}
                        <button 
                            onClick={handleReconnect}
                            className="bg-blue-500 text-white px-3 py-1 rounded-full hover:bg-blue-600"
                            disabled={isConnecting}
                        >
                            {isConnecting ? 'Connecting...' : 'Reconnect'}
                        </button>
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

                        {(showReconnecting && reconnectAttempts > 0) && (
                            <div className="absolute inset-0 flex items-center justify-center bg-black/50">
                                <div className="flex flex-col items-center space-y-4">
                                    <div className="w-12 h-12 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                                    <p className="text-white text-lg font-semibold">
                                        {`Reconnecting (Attempt ${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS})`}
                                    </p>
                                </div>
                            </div>
                        )}
                    </div>
                </div>

                {(startPoint || selectedRegion) && (
                    <div className="bg-blue-50 p-4 rounded-lg">
                        {startPoint && !selectedRegion && (
                            <>
                                <h3 className="font-semibold text-blue-700 mb-2">Selected Start Point</h3>
                                <div className="text-sm">
                                    <p>Position: {startPoint.x.toFixed(1)} x {startPoint.y.toFixed(1)}</p>
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
                        <p className="text-3xl font-bold mt-2">{details.fps.toFixed(1)}</p>
                        <p className="text-sm text-gray-500 mt-1">Video stream fps</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default VideoFeed;