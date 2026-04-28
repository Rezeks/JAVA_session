#!/bin/bash
# 🚀 БЫСТРЫЙ СТАРТ ПРОЕКТА

echo "=================================="
echo "Защищённая система обмена сообщениями"
echo "=================================="
echo ""

# Проверка Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven не найден в PATH"
    echo "   Установи Maven и добавь в PATH"
    exit 1
fi

# Проверка Java
if ! command -v java &> /dev/null; then
    echo "❌ Java не найдена в PATH"
    echo "   Required: Java 21+"
    exit 1
fi

echo "✅ Maven найден"
java -version
echo ""

# Компиляция
echo "📦 Компилирую проект..."
mvn -q -DskipTests clean compile
if [ $? -ne 0 ]; then
    echo "❌ Ошибка компиляции"
    exit 1
fi
echo "✅ Компиляция успешна"
echo ""

# Запуск тестов (опционально)
read -p "Запустить тесты? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🧪 Запускаю тесты..."
    mvn -q test
fi
echo ""

# Информация перед запуском
echo "=================================="
echo "ℹ️  ИНФОРМАЦИЯ ДО ЗАПУСКА"
echo "=================================="
echo ""
echo "1. Убедись, что Kafka запущена:"
echo "   Kafka должна быть доступна на localhost:9092"
echo ""
echo "2. После запуска откройте браузер:"
echo "   🌐 http://localhost:8080"
echo ""
echo "3. Документация:"
echo "   📖 START_HERE.md      - начните отсюда"
echo "   ⚡ EXECUTIVE_SUMMARY.md - быстрый ответ (2-5 мин)"
echo "   🎬 DEMO_SCENARIO.md   - пошаговая демонстрация"
echo ""
echo "4. API endpoints:"
echo "   👥 /api/users/*"
echo "   💬 /api/messages/*"
echo "   📁 /api/files/*"
echo "   📋 /api/audit"
echo ""

# Запуск приложения
echo "🚀 Запускаю приложение..."
echo "   (Ctrl+C для остановки)"
echo ""
echo "=================================="

mvn spring-boot:run

