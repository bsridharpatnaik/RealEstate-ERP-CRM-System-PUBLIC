// generate-manifest.js

const fs = require('fs-extra');
const dotenv = require('dotenv');
const path = require('path');

// Load environment variables from the appropriate .env file
const envFile = process.argv[2] || '.env';
dotenv.config({ path: path.resolve(__dirname, envFile) });

// Define the manifest content with environment variables
const manifest = {
  name: process.env.REACT_APP_NAME || 'My App',
  short_name: process.env.REACT_APP_NAME || 'App',
  start_url: process.env.REACT_APP_START_URL || '.',
  display: process.env.REACT_APP_DISPLAY || 'standalone',
  background_color: process.env.REACT_APP_BACKGROUND_COLOR || '#ffffff',
  theme_color: process.env.REACT_APP_THEME_COLOR || '#000000',
  description: process.env.REACT_APP_DESC || 'My awesome app',
  icons: [
    {
      src: process.env.REACT_APP_LOGIN_LOGO+".png" || '/path/to/default-icon-192.png',
      sizes: '192x192',
      type: 'image/png',
    },
    {
      src: process.env.REACT_APP_LOGIN_LOGO+".png" || '/path/to/default-icon-512.png',
      sizes: '512x512',
      type: 'image/png',
    },
  ],
};

// Write the manifest file to the public directory
fs.writeJson('./public/manifest.json', manifest, { spaces: 2 }, (err) => {
  if (err) {
    console.error('Error generating manifest.json:', err);
  } else {
    console.log(`manifest.json has been generated successfully for ${envFile}`);
  }
});
