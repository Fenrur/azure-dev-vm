DELETE FROM users;
INSERT INTO users (id, username, password, role, token) VALUES
    (-1, 'admin', '$2a$10$ZtDl9ZbpS0c6utKYmKnijeNQBKMp/pU7PmudQ.DPasvcKPJe0cUz6', 'admin', 0),
    (0, 'livio', '$2a$10$XyI4MSVsuScHCFuk3ZNZcOpzKZlKiAMGFvAPcROrjYfEdG2qswE6i', 'advanced', 10),
    (1, 'abou', '$2a$10$OYckHMAEKSiGh24bNMznvuSWPFdKF94i0hlTQps7y9AVRLxX7.f6G', 'basic', 10),
    (2, 'antho', '$2a$10$1rU2y2bKdp/YrQQCjioxYuCqmf.fJAyRl8f1CpbMszfdA0x.ErapK', 'basic', 0);

DELETE FROM virtual_machines;