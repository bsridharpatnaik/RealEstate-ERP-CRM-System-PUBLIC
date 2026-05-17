const camera = (function () {
  let width = 0;
  let height = 0;

  const createObjects = function () {};

  return {
    video: null,
    context: null,
    canvas: null,
    stream: null,

    startCamera: function (w = 680, h = 480) {
      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        width = w;
        height = h;

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
