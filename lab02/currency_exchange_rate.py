# lab02/currency_exchange_rate.py
import os
import sys
import argparse
import json
import logging
import requests
from datetime import datetime

def setup_logging(root_dir):
    """Настройка логирования для вывода в консоль и файл error.log в корне проекта."""
    log_file = os.path.join(root_dir, 'error.log')
    
    # Создаем кастомный логгер
    logger = logging.getLogger()
    logger.setLevel(logging.ERROR)
    
    formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
    
    # Обработчик для записи в файл
    fh = logging.FileHandler(log_file, encoding='utf-8')
    fh.setFormatter(formatter)
    logger.addHandler(fh)
    
    # Обработчик для вывода в консоль
    ch = logging.StreamHandler(sys.stdout)
    ch.setFormatter(formatter)
    logger.addHandler(ch)

def main():
    parser = argparse.ArgumentParser(description="Получение курсов обмена валют из API.")
    parser.add_argument('--from_curr', required=True, help="Базовая валюта (например, USD)")
    parser.add_argument('--to_curr', required=True, help="Целевая валюта (например, EUR)")
    parser.add_argument('--date', required=True, help="Дата в формате YYYY-MM-DD")
    parser.add_argument('--key', required=True, help="API Ключ для аутентификации")
    parser.add_argument('--url', default="http://localhost:8080/", help="Базовый URL API")
    
    args = parser.parse_args()

    # Определяем корневую папку проекта (на один уровень выше папки со скриптом)
    script_dir = os.path.dirname(os.path.abspath(__file__))
    root_dir = os.path.dirname(script_dir)
    
    setup_logging(root_dir)

    # Локальная проверка формата даты перед отправкой запроса
    try:
        datetime.strptime(args.date, "%Y-%m-%d")
    except ValueError:
        logging.error(f"Неверный формат даты: '{args.date}'. Ожидается YYYY-MM-DD.")
        sys.exit(1)

    try:
        # API требует 'key' в теле POST запроса, а 'from', 'to', 'date' в URL
        query_params = {
            'from': args.from_curr.upper(),
            'to': args.to_curr.upper(),
            'date': args.date
        }
        post_data = {
            'key': args.key
        }
        
        response = requests.post(args.url, params=query_params, data=post_data)
        response.raise_for_status()
        
        resp_json = response.json()
        
        # Проверка ошибок, которые возвращает сам API
        if resp_json.get('error'):
            logging.error(f"Ошибка API: {resp_json['error']}")
            sys.exit(1)
            
        rate_data = resp_json.get('data')
        if not rate_data:
            logging.error("Не получены данные от API.")
            sys.exit(1)
            
        # Создаем папку data в корне проекта, если она не существует
        data_dir = os.path.join(root_dir, 'data')
        os.makedirs(data_dir, exist_ok=True)
        
        # Сохранение JSON файла
        filename = f"{args.from_curr.upper()}_{args.to_curr.upper()}_{args.date}.json"
        filepath = os.path.join(data_dir, filename)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(rate_data, f, indent=4)
            
        print(f"Успех: Данные сохранены в {filepath}")
        
    except requests.exceptions.ConnectionError:
        logging.error(f"Ошибка подключения: Не удалось подключиться к {args.url}. Убедитесь, что Docker сервис запущен.")
        sys.exit(1)
    except requests.exceptions.RequestException as e:
        logging.error(f"Сбой HTTP запроса: {e}")
        sys.exit(1)
    except json.JSONDecodeError:
        logging.error("Не удалось прочитать JSON ответ от сервера.")
        sys.exit(1)
    except Exception as e:
        logging.error(f"Произошла непредвиденная ошибка: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()