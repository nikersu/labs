CREATE TABLE functions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    expression TEXT NOT NULL,
    user_id INTEGER NOT NULL,
    x_values TEXT,
    y_values TEXT,
    count INTEGER,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);