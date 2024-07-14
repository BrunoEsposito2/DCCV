import React from 'react';

const VideoFeed = ({ cameraId, url, details }) => {
  return (
      <div className="video-feed">
        <div className="video">
          <h2>Feed Video per {cameraId}</h2>
          <video src={url} controls autoPlay/>
        </div>
        <div className="details">
          <h3>Details</h3>
          <p>Numero di persone: {details.peopleCount}</p>
          <p>Colore dei vestiti: {details.clothesColor}</p>
        </div>
      </div>
  );
}

export default VideoFeed;
