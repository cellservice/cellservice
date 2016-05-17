-- Careful! The InsertEpsgSrid steps can take some time!
-- See http://northredoubt.com/n/2012/06/03/recent-spatialite-news-may-2012/

SELECT InitSpatialMetaData('NONE');
SELECT InsertEpsgSrid(4326); -- WGS 84 / Geographic [long-lat]; worldwide;
SELECT InsertEpsgSrid(32632); -- WGS 84 / UTM zone 32N
SELECT InsertEpsgSrid(32633); -- WGS 84 / UTM zone 33N
SELECT InsertEpsgSrid(25832); -- ETRS89 / UTM zone 32N
SELECT InsertEpsgSrid(25833); -- ETRS89 / UTM zone 33N


CREATE TABLE IF NOT EXISTS Cells (
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  cellid INTEGER,
  lac INTEGER,
  mnc INTEGER,
  mcc INTEGER,
  technology INTEGER
);

CREATE TABLE IF NOT EXISTS Measurements (
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  cell_id INTEGER REFERENCES Cells(id),
  provider TEXT,
  accuracy REAL,
  event TEXT,
  time INTEGER
);

SELECT AddGeometryColumn('Measurements', 'centroid', 4326, 'POINT', 'XY', 1);

CREATE TABLE IF NOT EXISTS Calls (
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  direction TEXT,
  address TEXT,
  starttime INTEGER,
  endtime INTEGER,
  startcell INTEGER REFERENCES Cells(id)
);

CREATE TABLE IF NOT EXISTS Handovers (
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  call_id REFERENCES Calls(id),
  startcell REFERENCES Cells(id),
  endcell REFERENCES Cells(id),
  time INTEGER
);

CREATE TABLE IF NOT EXISTS LocationUpdates (
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  startcell REFERENCES Cells(id),
  endcell REFERENCES Cells(id),
  time INTEGER
);

CREATE TABLE IF NOT EXISTS DataRecords (
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  rxbytes INTEGER,
  txbytes INTEGER,
  starttime INTEGER,
  endtime INTEGER,
  cell_id INTEGER REFERENCES Cells(id)
);

CREATE TABLE IF NOT EXISTS TextMessages (
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  direction TEXT,
  address TEXT,
  time INTEGER,
  cell_id INTEGER REFERENCES Cells(id)
);