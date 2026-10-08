FROM ubuntu:22.04

# Отключаем интерактивные запросы при установке пакетов
ENV DEBIAN_FRONTEND=noninteractive

# Устанавливаем cron, python, pip и dos2unix (для исправления переносов строк Windows)
RUN apt-get update && \
    apt-get install -y cron python3 python3-pip tzdata dos2unix && \
    rm -rf /var/lib/apt/lists/*

# Устанавливаем библиотеку requests
RUN pip3 install requests

# Создаем рабочую директорию
WORKDIR /app

# Копируем скрипты в контейнер
COPY currency_exchange_rate.py /app/
COPY entrypoint.sh /app/
COPY cronjob /etc/cron.d/currency-cron

# Исправляем виндовские переносы строк и назначаем права
RUN dos2unix /etc/cron.d/currency-cron /app/entrypoint.sh && \
    chmod 0644 /etc/cron.d/currency-cron && \
    chmod +x /app/entrypoint.sh

# Задаем часовой пояс
ENV TZ=Europe/Chisinau

ENTRYPOINT ["/app/entrypoint.sh"]