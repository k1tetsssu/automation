#!/bin/bash

# Проверка количества аргументов
if [ $# -lt 2 ] || [ $# -gt 3 ]; then
    echo "Ошибка: необходимо указать 2 или 3 аргумента."
    echo "Использование: $0 <путь_к_каталогу> <макс_объем_МБ> [порог_%]"
    exit 1
fi

DIRECTORY="$1"
MAX_SIZE_MB="$2"
THRESHOLD="${3:-80}"

EMAIL="admin@example.com"

# Проверка существования каталога
if [ ! -d "$DIRECTORY" ]; then
    echo "Ошибка: каталог '$DIRECTORY' не существует."
    exit 1
fi

# Проверка максимального объёма
if ! [[ "$MAX_SIZE_MB" =~ ^[0-9]+$ ]]; then
    echo "Ошибка: максимальный объем должен быть целым числом в МБ."
    exit 1
fi

# Проверка порога
if ! [[ "$THRESHOLD" =~ ^[0-9]+$ ]] ||
   [ "$THRESHOLD" -lt 1 ] ||
   [ "$THRESHOLD" -gt 100 ]; then
    echo "Ошибка: порог должен быть числом от 1 до 100."
    exit 1
fi

# Получение размера каталога в МБ
USED_MB=$(du -sm "$DIRECTORY" | awk '{print $1}')

# Расчёт процента использования
USAGE_PERCENT=$((USED_MB * 100 / MAX_SIZE_MB))

# Ограничение значения до 100%
if [ "$USAGE_PERCENT" -gt 100 ]; then
    USAGE_PERCENT=100
fi

# Дата и время
DATE=$(date '+%Y-%m-%d %H:%M:%S')

# Запись результата в лог
echo "$DATE | Directory: $DIRECTORY | Usage: ${USAGE_PERCENT}% | Used: ${USED_MB} MB / ${MAX_SIZE_MB} MB" >> disk_usage.log

# Вывод результата
echo "Каталог: $DIRECTORY"
echo "Использовано: ${USED_MB} MB из ${MAX_SIZE_MB} MB"
echo "Использование дискового пространства: ${USAGE_PERCENT}%"

# Проверка превышения порога
if [ "$USAGE_PERCENT" -ge "$THRESHOLD" ]; then
    echo "ВНИМАНИЕ: использование дискового пространства достигло ${USAGE_PERCENT}%."

    echo "Внимание! Использование каталога '$DIRECTORY' достигло ${USAGE_PERCENT}%." \
        | mail -s "Disk usage warning" "$EMAIL"
fi