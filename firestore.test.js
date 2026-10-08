const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test("Unauthenticated access rejected on sessions", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthedDb.collection("users").doc(ALICE_UID).collection("sessions").get()
  );
});

test("Alice can create and read her own gameplay dataset session", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const sessionDoc = aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("session_1");

  await assertSucceeds(
    sessionDoc.set({
      userId: ALICE_UID,
      pilotCallsign: "JAMESON",
      createdAt: new Date(),
      updatedAt: new Date(),
      totalKills: 4,
      accuracyRate: 68.5,
      status: "ACTIVE",
      laserShotsFired: 120,
      laserHits: 82,
      sampleCount: 15
    })
  );

  await assertSucceeds(sessionDoc.get());
});

test("Bob cannot read or write Alice's dataset session", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const sessionDocAlice = aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("session_1");
  await sessionDocAlice.set({
    userId: ALICE_UID,
    pilotCallsign: "JAMESON",
    createdAt: new Date(),
    updatedAt: new Date(),
    totalKills: 10,
    accuracyRate: 75.0,
    status: "ACTIVE"
  });

  const bobAccessDoc = bobDb.collection("users").doc(ALICE_UID).collection("sessions").doc("session_1");
  await assertFails(bobAccessDoc.get());
  await assertFails(
    bobAccessDoc.set({
      userId: BOB_UID,
      pilotCallsign: "HACKER",
      createdAt: new Date(),
      updatedAt: new Date(),
      totalKills: 0,
      accuracyRate: 0.0,
      status: "ACTIVE"
    })
  );
});

test("Alice can record telemetry frames under her session", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const telemetryDoc = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("sessions")
    .doc("session_1")
    .collection("telemetry")
    .doc("frame_1");

  await assertSucceeds(
    telemetryDoc.set({
      userId: ALICE_UID,
      sessionId: "session_1",
      eventType: "COMBAT_LASER_PULSE",
      timestamp: new Date(),
      combatRank: "DEADLY",
      outcomeSuccess: true,
      targetShipType: "COBRA_MK_3",
      playerShipSpeed: 32.5,
      playerShields: 85.0,
      playerEnergy: 92.0,
      targetDistance: 450.0,
      tacticalAction: "PURSUIT_PITCH_YAW_ALIGN",
      isAutoPlay: true
    })
  );
});
