const camera = (function () {
  const createObjects = function () {};

  return {
    video: null,
    context: null,
    canvas: null,
    stream: null,

    startCamera: function () {
      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        createObjects();

        this.video = document.getElementById("video");
        this.canvas = document.getElementById("canvas");
        this.context = this.canvas.getContext("2d");
        var mediaSupport = "mediaDevices" in navigator;

        if (mediaSupport) {
          ((video) => {
            navigator.mediaDevices
              .getUserMedia({ video: true })
              .then((stream) => {
                this.stream = stream;
                video.srcObject = stream;
                video.play();
              })
              .catch(function (err) {
                window.alert("Unable to access camera: " + err);
              });
          })(this.video);
        } else {
          window.alert("Your browser does not support media devices.");
          return;
        }
      }
    },
    stopCamera: function () {
      this.stream.getTracks().forEach((track) => {
        track.stop();
      });
    },
    getImage: function () {
      return this.canvas.toDataURL("image/png");
    },
    takeSnapshot: function () {
      this.context.drawImage(
        this.video,
        0,
        0,
        this.video.offsetWidth,
        this.video.offsetHeight
      );
    },
  };
})();

export default camera;
