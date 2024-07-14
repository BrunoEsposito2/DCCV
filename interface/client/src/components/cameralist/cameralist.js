import React from 'react';

const CameraList = ({ cameras, onCameraClick }) => {
  return (
      <div className="camera-list">
        {cameras.map((camera) => (
            <button key={camera.id} onClick={() => onCameraClick(camera.id)}>
              {camera.name}
            </button>
        ))}
      </div>
  );
}

export default CameraList;
