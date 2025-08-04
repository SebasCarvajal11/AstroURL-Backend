# AstroURL Backend

Backend service for AstroURL, a thematic URL shortener.

## Stack Tecnológico
- **Lenguaje**: Java 21
- **Framework**: Spring Boot 3.2.5
- **Gestor de Dependencias**: Maven

## Dependencias Clave y Justificación

### Framework y Web
- **Spring Boot Starter Web**: Núcleo para la creación de aplicaciones web y APIs RESTful.
- **Spring Boot Starter Security**: Proporciona la infraestructura de seguridad para proteger los endpoints, manejar la autenticación y autorización.
- **Spring Boot Starter Validation**: Utilizado para la validación de DTOs y parámetros de entrada, crucial para la seguridad y la integridad de los datos.

### Persistencia y Datos
- **Spring Boot Starter Data JPA**: Facilita la implementación de la capa de acceso a datos con el patrón Repository y el ORM Hibernate.
- **Spring Boot Starter Data Redis**: Integra Redis para caching, gestión de sesiones y tareas de alto rendimiento como el rate limiting.
- **MySQL Connector/J**: Driver JDBC para la comunicación con la base de datos MySQL.
- **Liquibase**: Herramienta para la gestión de migraciones de la base de datos de forma versionada y controlada, esencial para el despliegue continuo.

### Seguridad Adicional
- **io.jsonwebtoken (jjwt)**: Biblioteca estándar para la creación y validación de JSON Web Tokens (JWT), la base de nuestra autenticación stateless.

### Utilidades y Herramientas
- **Lombok**: Reduce significativamente el código boilerplate (getters, setters, constructores), haciendo el código más limpio y legible.
- **Spring Boot DevTools**: Mejora la experiencia de desarrollo con reinicios automáticos y recarga en caliente (hot-reloading).
- **ZXing (Core & JavaSE)**: Librerías para la generación de códigos QR, una funcionalidad clave para los enlaces acortados.
- **Apache PDFBox & Commons CSV**: Utilizadas para la generación de reportes en PDF y CSV, funcionalidades premium del plan Sirio.
- **SpringDoc OpenAPI**: Genera automáticamente la documentación de la API en formato OpenAPI 3, facilitando la integración con frontends y otros servicios.

### Pruebas
- **Spring Boot Starter Test**: Proporciona las herramientas base para las pruebas unitarias y de integración (JUnit 5, Mockito).
- **Testcontainers**: Permite levantar contenedores Docker (MySQL, Redis) durante las pruebas de integración, garantizando un entorno de prueba limpio y consistente que replica la producción.