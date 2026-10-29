-- Cada usuario de prueba pasa a tener su propia contraseña: <usuario>123 (hash BCrypt, costo 10).
UPDATE usuarios SET password = '$2a$10$A8qb36H8UuciF1LoP3nNRed9fS6FxNdp2/cTg6xsyWqR6OorPN6RS' WHERE username = 'admin';
UPDATE usuarios SET password = '$2a$10$c14amoRtPya0Hv9QglKE4.i/XMdWuOSLUIzVm5azvCuscSOIweZDK' WHERE username = 'asesor';
UPDATE usuarios SET password = '$2a$10$XRdSRdVmYbkyZ1CNu7nXyuy7.UtrCAg8K7/oAnG2wNelOMsLwx56C' WHERE username = 'jefe';
UPDATE usuarios SET password = '$2a$10$9Gy2uqmo6wq4u9MuDzMR4uU28UNYjaK61jDc/1XEdasc3/g/2/Wxi' WHERE username = 'inactivo';
