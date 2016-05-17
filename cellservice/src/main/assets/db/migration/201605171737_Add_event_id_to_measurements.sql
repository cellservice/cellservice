CREATE TABLE IF NOT EXISTS EventTypes(
    id INTEGER PRIMARY KEY NOT NULL,
    name TEXT NOT NULL
);
INSERT INTO EventTypes(id, name) VALUES
    (1, 'Call'),
    (2, 'Handover'),
    (3, 'Location update'),
    (4, 'Data record'),
    (5, 'Text message'),
    (-1, 'Unknown');

ALTER TABLE Measurements RENAME TO MeasurementsOld;

CREATE TABLE IF NOT EXISTS Measurements (
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  cell_id INTEGER REFERENCES Cells(id),
  provider TEXT NOT NULL,
  accuracy REAL NOT NULL,
  time INTEGER NOT NULL,
  event_id INTEGER NOT NULL,
  event_type INTEGER NOT NULL REFERENCES EventTypes(id)
);
SELECT AddGeometryColumn('Measurements', 'centroid', 4326, 'POINT', 'XY', 1);

INSERT INTO Measurements SELECT id, cell_id, provider, accuracy, time, -1 as event_id, CASE
                              WHEN event = 'call' THEN 1
                              WHEN event LIKE '%handover%' THEN 2
                              WHEN event LIKE '%location update%' THEN 3
                              WHEN event = 'data' THEN 4
                              WHEN event = 'text message' THEN 5
                              ELSE -1
                           END as event_type, centroid
                         FROM MeasurementsOld
                         ORDER BY id;

DROP TABLE MeasurementsOld;