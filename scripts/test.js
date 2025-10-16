// Comprehensive test script for GeoJSON to KML conversion API
// Usage:
//   1. Start the application: ./gradlew bootRun
//   2. In scripts directory: npm init -y && npm i ws axios
//   3. Run: node test.js

const WebSocket = require('ws');
const axios = require('axios');

const BASE = process.env.BASE || 'http://localhost:8080';
const WS = process.env.WS || 'ws://localhost:8080/ws';
const userId = 'user123';

// Hawaii GeoJSON data
const hawaiiGeoJson = {
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {},
      "geometry": {
        "coordinates": [
          [-155.06244948820577, 19.709586535142037],
          [-156.4833403856122, 20.888261526592686],
          [-157.8855316531354, 21.329531202461226],
          [-159.63423707551948, 22.087717066899188],
          [-157.96445445049062, 21.628041397671836]
        ],
        "type": "LineString"
      }
    },
    {
      "type": "Feature",
      "properties": {},
      "geometry": {
        "coordinates": [-156.88909163043272, 21.253909984035786],
        "type": "Point"
      }
    },
    {
      "type": "Feature",
      "properties": {},
      "geometry": {
        "coordinates": [-155.6696280495238, 19.347076690429716],
        "type": "Point"
      }
    },
    {
      "type": "Feature",
      "properties": {},
      "geometry": {
        "coordinates": [
          [-156.25116772904565, 20.865618851751222],
          [-157.86357728239358, 21.50208906922174],
          [-156.3607932456932, 20.705422203699257],
          [-156.20235579732437, 20.934295643033423],
          [-155.91982441707106, 21.760312585953017],
          [-157.9009707068477, 21.524861575914528]
        ],
        "type": "LineString"
      }
    },
    {
      "type": "Feature",
      "properties": {},
      "geometry": {
        "coordinates": [
          [
            [-154.94942490411023, 21.482088917778185],
            [-154.75198537592985, 20.438512560352862],
            [-152.46119812554636, 21.345258357242002],
            [-153.5635282594922, 23.03861819606732],
            [-155.8905167749056, 23.701207334651045],
            [-155.61895260390716, 22.823929510785916],
            [-154.94942490411023, 21.482088917778185]
          ]
        ],
        "type": "Polygon"
      }
    }
  ]
};

const createdJobIds = [];
const wsMessages = [];

// Helper function to delay
const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// Helper function to log responses nicely
function logResponse(title, data) {
  console.log('\n' + '='.repeat(80));
  console.log(title);
  console.log('='.repeat(80));
  console.log(JSON.stringify(data, null, 2));
}

(async () => {
  console.log('\n🚀 Starting Comprehensive API Test Suite');
  console.log('━'.repeat(80));

  // Step 1: Connect WebSocket
  console.log('\n📡 Step 1: Connecting WebSocket...');
  const ws = new WebSocket(`${WS}?userId=${userId}`);

  ws.on('open', async () => {
    console.log('✅ WebSocket connected successfully');

    try {
      // Step 2: Create 3 conversion jobs
      console.log('\n📤 Step 2: Creating 3 conversion jobs...');
      for (let i = 1; i <= 3; i++) {
        const response = await axios.post(`${BASE}/api/job/convert`, {
          userId: userId,
          geo: hawaiiGeoJson
        });
        createdJobIds.push(response.data.jobId);
        logResponse(`Convert Job ${i} Response (202 ACCEPTED)`, response.data);

        // Small delay between requests
        await delay(100);
      }

      console.log(`\n✅ Created ${createdJobIds.length} jobs: ${createdJobIds.join(', ')}`);

      // Step 3: Wait a bit for processing
      console.log('\n⏳ Step 3: Waiting for jobs to process (2 seconds)...');
      await delay(2000);

      // Step 4: Get job status by ID (first job)
      console.log(`\n📊 Step 4: Getting job status for first job: ${createdJobIds[0]}`);
      try {
        const statusResponse = await axios.get(`${BASE}/api/job/${createdJobIds[0]}`);
        logResponse('Get Job Status Response', statusResponse.data);
      } catch (err) {
        console.error('❌ Error getting job status:', err.response?.data || err.message);
      }

      // Step 5: Get all jobs
      console.log('\n📋 Step 5: Getting all jobs...');
      try {
        const allJobsResponse = await axios.get(`${BASE}/api/job`);
        logResponse('Get All Jobs Response', allJobsResponse.data);
      } catch (err) {
        console.error('❌ Error getting all jobs:', err.response?.data || err.message);
      }

      // Step 6: Get jobs by status - PENDING
      console.log('\n🔍 Step 6: Getting jobs by status - PENDING');
      try {
        const pendingResponse = await axios.get(`${BASE}/api/job?status=PENDING`);
        logResponse('Jobs with PENDING status', pendingResponse.data);
      } catch (err) {
        console.error('❌ Error getting PENDING jobs:', err.response?.data || err.message);
      }

      // Step 7: Get jobs by status - PROCESSING
      console.log('\n🔍 Step 7: Getting jobs by status - PROCESSING');
      try {
        const processingResponse = await axios.get(`${BASE}/api/job?status=PROCESSING`);
        logResponse('Jobs with PROCESSING status', processingResponse.data);
      } catch (err) {
        console.error('❌ Error getting PROCESSING jobs:', err.response?.data || err.message);
      }

      // Step 8: Get jobs by status - DONE
      console.log('\n🔍 Step 8: Getting jobs by status - DONE');
      try {
        const doneResponse = await axios.get(`${BASE}/api/job?status=DONE`);
        logResponse('Jobs with DONE status', doneResponse.data);
      } catch (err) {
        console.error('❌ Error getting DONE jobs:', err.response?.data || err.message);
      }

      // Step 9: Get jobs by status - FAILED
      console.log('\n🔍 Step 9: Getting jobs by status - FAILED');
      try {
        const failedResponse = await axios.get(`${BASE}/api/job?status=FAILED`);
        logResponse('Jobs with FAILED status', failedResponse.data);
      } catch (err) {
        console.error('❌ Error getting FAILED jobs:', err.response?.data || err.message);
      }

      // Step 10: Get job files (first job)
      console.log(`\n📄 Step 10: Getting job files for first job: ${createdJobIds[0]}`);
      try {
        const filesResponse = await axios.get(`${BASE}/api/job/${createdJobIds[0]}/files`);
        logResponse('Get Job Files Response', {
          jobId: filesResponse.data.jobId,
          geoRecordId: filesResponse.data.geoRecordId,
          status: filesResponse.data.status,
          geoJson: filesResponse.data.geoJson ? '(GeoJSON data present)' : null,
          kml: filesResponse.data.kml ? '(KML data present - truncated for display)' : null,
          kmlPreview: filesResponse.data.kml ? filesResponse.data.kml.substring(0, 200) + '...' : null
        });
      } catch (err) {
        console.error('❌ Error getting job files:', err.response?.data || err.message);
      }

      // Step 11: Download KML (first job)
      console.log(`\n📥 Step 11: Downloading KML for first job: ${createdJobIds[0]}`);
      try {
        const kmlResponse = await axios.get(`${BASE}/api/job/${createdJobIds[0]}/kml`);
        console.log('\n' + '='.repeat(80));
        console.log('Download KML Response');
        console.log('='.repeat(80));
        console.log('Content-Type:', kmlResponse.headers['content-type']);
        console.log('Content-Disposition:', kmlResponse.headers['content-disposition']);
        console.log('\nKML Content (first 500 chars):');
        console.log('─'.repeat(80));
        console.log(kmlResponse.data.substring(0, 500));
        console.log('...');
        console.log('─'.repeat(80));
      } catch (err) {
        console.error('❌ Error downloading KML:', err.response?.data || err.message);
      }

      // Step 12: Delete first job
      console.log(`\n🗑️  Step 12: Deleting first job: ${createdJobIds[0]}`);
      try {
        const deleteResponse = await axios.delete(`${BASE}/api/job/${createdJobIds[0]}`);
        logResponse('Delete Job Response', deleteResponse.data);
      } catch (err) {
        console.error('❌ Error deleting job:', err.response?.data || err.message);
      }

      // Step 13: Try to get deleted job status (should fail)
      console.log(`\n🔍 Step 13: Attempting to get status of deleted job: ${createdJobIds[0]}`);
      try {
        const deletedJobResponse = await axios.get(`${BASE}/api/job/${createdJobIds[0]}`);
        logResponse('Get Deleted Job Response (unexpected success)', deletedJobResponse.data);
      } catch (err) {
        if (err.response?.status === 404) {
          console.log('✅ Expected 404 error - job was successfully deleted');
          logResponse('Error Response', err.response.data);
        } else {
          console.error('❌ Unexpected error:', err.response?.data || err.message);
        }
      }

      // Step 14: Display all WebSocket messages
      console.log('\n📨 Step 14: WebSocket Messages Received');
      console.log('─'.repeat(80));
      console.log(`Total messages received: ${wsMessages.length}`);
      wsMessages.forEach((msg, index) => {
        logResponse(`WebSocket Message ${index + 1}`, msg);
      });

      // Summary
      console.log('\n' + '━'.repeat(80));
      console.log('✅ Test Suite Completed Successfully!');
      console.log('━'.repeat(80));
      console.log('\nSummary:');
      console.log(`  • Jobs Created: ${createdJobIds.length}`);
      console.log(`  • WebSocket Messages: ${wsMessages.length}`);
      console.log(`  • First Job ID: ${createdJobIds[0]}`);
      console.log(`  • Deleted Job ID: ${createdJobIds[0]}`);
      console.log('\n');

      ws.close();
      process.exit(0);

    } catch (err) {
      console.error('\n❌ Fatal error during test execution:', err.message);
      console.error(err.stack);
      ws.close();
      process.exit(1);
    }
  });

  ws.on('message', (data) => {
    const msg = JSON.parse(data.toString());
    wsMessages.push(msg);
    console.log(`\n📩 WebSocket message received: ${JSON.stringify(msg)}`);
  });

  ws.on('error', (err) => {
    console.error('\n❌ WebSocket error:', err.message);
  });

  ws.on('close', () => {
    console.log('\n📡 WebSocket connection closed');
  });

  // Timeout after 30 seconds
  setTimeout(() => {
    console.error('\n❌ Test timed out after 30 seconds');
    ws.close();
    process.exit(1);
  }, 30000);
})();
