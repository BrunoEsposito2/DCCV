// Script per inizializzare MongoDB
db = db.getSiblingDB('DCCV');
db.createUser({
    user: 'APP-USERNAME',
    pwd: 'APP-PASSWORD',
    roles: [{ role: 'readWrite', db: 'DCCV' }]
});
db.createCollection('tracking');