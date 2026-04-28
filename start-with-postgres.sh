#!/usr/bin/env bash
# 🚀 БЫСТРЫЙ СТАРТ: PostgreSQL + Методы шифрования

set -e

echo "================================="
echo "🚀 Защищённая система обмена сообщениями"
echo "   с PostgreSQL + Сравнение методов шифрования"
echo "================================="
echo ""

# ШАЕГ 1: Проверка Docker и Docker Compose
echo "1️⃣  Проверяю наличие Docker..."
if ! command -v docker &> /dev/null; then
    echo "❌ Docker не найден"
    echo "   Установи Docker с https://www.docker.com/products/docker-desktop"
    exit 1
fi
echo "✅ Docker найден"

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose не найден"
    echo "   Обновь Docker или установи docker-compose отдельно"
    exit 1
fi
echo "✅ Docker Compose найден"
echo ""

# ШАГ 2: Проверка Maven
echo "2️⃣  Проверяю наличие Maven и Java..."
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven не найден в PATH"
    echo "   Установи Maven и добавь в PATH"
    exit 1
fi
echo "✅ Maven найден"

java -version 2>&1 | head -1
echo ""

# ШАГ 3: Запуск Docker Compose
echo "3️⃣  Запускаю Kafka + PostgreSQL..."
docker-compose up -d

echo "⏳ Даю 10 секунд чтобы сервисы загрузились..."
sleep 10

echo "✅ Сервисы запущены"
echo ""

# ШАГ 4: Компиляция
echo "4️⃣  Компилирую проект..."
mvn -q -DskipTests clean compile

if [ $? -ne 0 ]; then
    echo "❌ Ошибка компиляции"
    exit 1
fi
echo "✅ Проект скомпилирован"
echo ""

# ШАГ 5: Запуск приложения
echo "5️⃣  Запускаю приложение..."
echo ""
echo "════════════════════════════════════════════════"
echo "🚀 ПРИЛОЖЕНИЕ ЗАПУЩЕНО"
echo "════════════════════════════════════════════════"
echo ""
echo "🌐 Откройте браузер и перейдите на:"
echo ""
echo "   💻 http://localhost:8080"
echo ""
echo "   🔐 Сравнение методов шифрования:"
echo "      http://localhost:8080/ui/encryption-comparison.html"
echo ""
echo "📊 Kafka:      localhost:9092"
echo "🗄️  PostgreSQL:  localhost:5432"
echo ""
echo "❌ Для остановки: Ctrl+C"
echo "════════════════════════════════════════════════"
echo ""

# Запускаем приложение
mvn spring-boot:run

