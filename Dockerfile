##########################################
## Dockerfile para el uso del Servidor  ##
## Basado en Ubuntu 14.04               ##
##########################################

# Setea la imagen base (Ubuntu oficial, versión 14.04)
FROM ubuntu:14.04

# Autor: mart / Mantiene: mart
MAINTAINER mart mart

# Copio los directorios del repositorio
COPY ./ /home

# Uso el script de instalación con parámetro para Docker
RUN cd /home/server && chmod 777 server_install_v0.2.sh && ./server_install_v0.2.sh -docker

# Defino el directorio de trabajo
WORKDIR /home

# Defino el comando estándar
CMD ["bash"]
