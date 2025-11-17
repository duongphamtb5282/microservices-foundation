// MongoDB initialization script for customer service
// This script runs when the MongoDB container starts for the first time

print('🚀 Starting MongoDB initialization...');

// Create customeruser in admin database
db = db.getSiblingDB('admin');

try {
  // Check if customeruser already exists
  const existingUser = db.getUser('customeruser');
  if (!existingUser) {
    db.createUser({
      user: 'customeruser',
      pwd: 'customerpass',
      roles: [
        { role: 'readWrite', db: 'customerdb' },
        { role: 'dbAdmin', db: 'customerdb' }
      ]
    });
    print('✅ Created customeruser');
  } else {
    print('ℹ️  customeruser already exists');
  }
} catch (e) {
  print('❌ Error creating customeruser:', e.message);
}

// Switch to customerdb and set up collections
db = db.getSiblingDB('customerdb');

try {
  // Create collections
  if (!db.getCollectionNames().includes('customers')) {
    db.createCollection('customers');
    print('✅ Created customers collection');
  } else {
    print('ℹ️  customers collection already exists');
  }

  // Create indexes
  db.customers.createIndex({ "email": 1 }, { unique: true, name: "idx_email_unique" });
  db.customers.createIndex({ "profile.firstName": 1, "profile.lastName": 1 }, { name: "idx_name" });
  db.customers.createIndex({ "status": 1 }, { name: "idx_status" });
  db.customers.createIndex({ "createdAt": 1 }, { name: "idx_created_at" });
  db.customers.createIndex({ "updatedAt": -1 }, { name: "idx_updated_at" });

  print('✅ Created indexes');
} catch (e) {
  print('❌ Error setting up collections:', e.message);
}

print('✅ MongoDB initialization completed for customer service');
