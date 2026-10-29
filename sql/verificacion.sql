-- Ejecutar conectado como AUTODB. Consultas de lectura para la defensa.
SELECT "version", "description", "success" FROM "flyway_schema_history" ORDER BY "installed_rank";

SELECT 'CLIENTES' recurso, COUNT(*) cantidad FROM cliente
UNION ALL SELECT 'SERVICIOS', COUNT(*) FROM servicio
UNION ALL SELECT 'VEHICULOS', COUNT(*) FROM vehiculo
UNION ALL SELECT 'ORDENES', COUNT(*) FROM orden_servicio
UNION ALL SELECT 'DETALLES', COUNT(*) FROM detalle_orden;

SELECT id, codigo, nombre, precio, activo FROM servicio ORDER BY codigo;
SELECT v.id, v.placa, c.nombres cliente FROM vehiculo v JOIN cliente c ON c.id = v.cliente_id ORDER BY v.placa;

-- Cabecera y detalle: sustituir el numero por el devuelto al registrar.
SELECT o.numero, o.fecha, v.placa, o.kilometraje, o.estado, o.total, o.usuario_registro,
       s.codigo, d.cantidad, d.precio_unitario, d.subtotal
FROM orden_servicio o
JOIN vehiculo v ON v.id = o.vehiculo_id
JOIN detalle_orden d ON d.orden_id = o.id
JOIN servicio s ON s.id = d.servicio_id
ORDER BY o.id DESC, d.id;

-- No debe devolver filas: el total coincide con la suma de detalles.
SELECT o.numero, o.total, SUM(d.subtotal) suma
FROM orden_servicio o JOIN detalle_orden d ON d.orden_id = o.id
GROUP BY o.numero, o.total
HAVING o.total <> SUM(d.subtotal);

-- Comparar estos conteos antes y despues de intentar registrar un tercer detalle SRV-008.
-- Ambos deben permanecer iguales.
SELECT COUNT(*) cabeceras FROM orden_servicio;
SELECT COUNT(*) detalles FROM detalle_orden;

-- Resultado equivalente al reporte JPQL: excluye ANULADAS.
SELECT s.codigo, s.nombre servicio, SUM(d.cantidad) cantidad_total, SUM(d.subtotal) importe_total
FROM detalle_orden d
JOIN servicio s ON s.id = d.servicio_id
JOIN orden_servicio o ON o.id = d.orden_id
WHERE o.estado = 'REGISTRADA'
GROUP BY s.codigo, s.nombre
ORDER BY importe_total DESC, s.codigo;

-- Demostracion del hash sin mostrarlo completo.
SELECT username, SUBSTR(password, 1, 7) prefijo_bcrypt, LENGTH(password) longitud, activo
FROM usuarios ORDER BY username;

