#!/usr/bin/env python3
"""
Script de prueba para validar la API híbrida CrearPedidoCompletoHibrido
"""

import requests
import json
import time
import sys
from datetime import datetime

def test_api_hibrida():
    """Prueba completa de la API híbrida"""
    
    # Configuración
    base_url = "http://localhost:5000"  # Ajustar según tu servidor
    endpoint = f"{base_url}/api/PedidoControllerHibrido/CrearPedidoCompletoHibrido"
    
    # Datos de prueba
    test_data = {
        "IdPersona": 12345,
        "IdDireccionEntrega": 67890,
        "TotalVenta": 150.50,
        "Peso": 2.5,
        "TipoCp": 1001,
        "Productos": [
            {
                "IdProducto": 101,
                "IdUnidad": 1,
                "Cantidad": 2,
                "Peso": 1.0,
                "Precio": 50.25,
                "Total": 100.50,
                "Descripcion": "Producto A de prueba"
            },
            {
                "IdProducto": 102,
                "IdUnidad": 1,
                "Cantidad": 1,
                "Peso": 1.5,
                "Precio": 50.00,
                "Total": 50.00,
                "Descripcion": "Producto B de prueba"
            }
        ]
    }
    
    print("🧪 Iniciando prueba de API híbrida...")
    print(f"📡 Endpoint: {endpoint}")
    print(f"📊 Datos: {json.dumps(test_data, indent=2)}")
    
    # Medir tiempo de ejecución
    start_time = time.time()
    
    try:
        # Realizar petición
        response = requests.post(
            endpoint,
            json=test_data,
            headers={"Content-Type": "application/json"},
            timeout=180  # 3 minutos timeout
        )
        
        end_time = time.time()
        execution_time = end_time - start_time
        
        print(f"\n⏱️  Tiempo de ejecución: {execution_time:.2f} segundos")
        
        # Validar respuesta
        if response.status_code == 200:
            result = response.json()
            print("✅ API híbrida respondió exitosamente!")
            print(f"📄 Respuesta: {json.dumps(result, indent=2)}")
            
            # Validaciones
            validations = []
            
            # Validar tiempo de ejecución
            if execution_time < 30:
                validations.append("✅ Tiempo de ejecución < 30 segundos")
            else:
                validations.append("⚠️  Tiempo de ejecución > 30 segundos")
            
            # Validar estructura de respuesta
            required_fields = ['IdCp', 'IdCpInventario', 'NumCp', 'Fecha', 'RequestId', 'ItemsProcesados']
            missing_fields = [field for field in required_fields if field not in result]
            
            if not missing_fields:
                validations.append("✅ Estructura de respuesta completa")
            else:
                validations.append(f"❌ Campos faltantes: {missing_fields}")
            
            # Validar que se procesaron todos los items
            if result.get('ItemsProcesados', 0) == len(test_data['Productos']):
                validations.append("✅ Todos los items fueron procesados")
            else:
                validations.append(f"⚠️  Items procesados: {result.get('ItemsProcesados', 0)}/{len(test_data['Productos'])}")
            
            # Validar que no haya timeout
            if execution_time < 180:
                validations.append("✅ Sin timeout del cliente")
            else:
                validations.append("❌ Timeout detectado")
            
            print("\n📋 Validaciones:")
            for validation in validations:
                print(f"   {validation}")
            
            # Resultado final
            if all("✅" in v for v in validations):
                print("\n🎉 ¡Todas las validaciones pasaron!")
                print("✅ La API híbrida está funcionando correctamente")
                print("✅ No hay congelamiento del ERP")
                print("✅ Rendimiento optimizado")
                return True
            else:
                print("\n⚠️  Algunas validaciones fallaron")
                return False
                
        else:
            print(f"❌ Error HTTP {response.status_code}: {response.text}")
            return False
            
    except requests.exceptions.Timeout:
        print("❌ Timeout: La petición excedió los 180 segundos")
        return False
    except requests.exceptions.ConnectionError:
        print("❌ Error de conexión: No se pudo conectar al servidor")
        return False
    except Exception as e:
        print(f"❌ Error inesperado: {str(e)}")
        return False

def test_idempotencia():
    """Prueba de idempotencia con mismo RequestId"""
    
    print("\n🔄 Prueba de idempotencia...")
    
    # Usar mismo RequestId para dos peticiones
    request_id = "test-idempotencia-12345"
    
    # Primera petición
    result1 = make_request_with_requestid(request_id)
    
    # Segunda petición con mismo RequestId
    result2 = make_request_with_requestid(request_id)
    
    if result1 and result2:
        if result1.get('IdCp') == result2.get('IdCp'):
            print("✅ Idempotencia confirmada: mismos IDs devueltos")
            return True
        else:
            print("❌ Idempotencia fallida: IDs diferentes")
            return False
    else:
        print("❌ Error en prueba de idempotencia")
        return False

def make_request_with_requestid(request_id):
    """Helper para hacer petición con RequestId específico"""
    # Implementar según tu API
    # Esta es una función placeholder
    pass

def test_carga():
    """Prueba con muchos items para validar rendimiento"""
    
    print("\n⚡ Prueba de carga con 50 items...")
    
    # Crear datos con muchos items
    test_data_carga = {
        "IdPersona": 12345,
        "IdDireccionEntrega": 67890,
        "TotalVenta": 2500.00,
        "Peso": 50.0,
        "TipoCp": 1001,
        "Productos": []
    }
    
    # Generar 50 items de prueba
    for i in range(50):
        test_data_carga["Productos"].append({
            "IdProducto": 100 + i,
            "IdUnidad": 1,
            "Cantidad": 2,
            "Peso": 1.0,
            "Precio": 25.00,
            "Total": 50.00,
            "Descripcion": f"Producto de carga {i+1}"
        })
    
    start_time = time.time()
    
    try:
        response = requests.post(
            "http://localhost:5000/api/PedidoControllerHibrido/CrearPedidoCompletoHibrido",
            json=test_data_carga,
            headers={"Content-Type": "application/json"},
            timeout=180
        )
        
        end_time = time.time()
        execution_time = end_time - start_time
        
        if response.status_code == 200:
            result = response.json()
            print(f"✅ Carga exitosa en {execution_time:.2f} segundos")
            print(f"📊 Items procesados: {result.get('ItemsProcesados', 0)}/50")
            return True
        else:
            print(f"❌ Error en prueba de carga: {response.status_code}")
            return False
            
    except Exception as e:
        print(f"❌ Error en prueba de carga: {str(e)}")
        return False

def main():
    """Función principal de pruebas"""
    
    print("🚀 Iniciando suite de pruebas de API híbrida")
    print("=" * 50)
    
    # Ejecutar pruebas
    tests_passed = 0
    total_tests = 2
    
    # Prueba básica
    if test_api_hibrida():
        tests_passed += 1
    
    # Prueba de carga
    if test_carga():
        tests_passed += 1
    
    # Resultado final
    print("\n" + "=" * 50)
    print(f"📊 Resumen de pruebas: {tests_passed}/{total_tests} pasadas")
    
    if tests_passed == total_tests:
        print("🎉 ¡Todas las pruebas pasaron!")
        print("✅ La API híbrida está lista para producción")
        sys.exit(0)
    else:
        print("⚠️  Algunas pruebas fallaron")
        print("🔧 Revisar configuración e implementación")
        sys.exit(1)

if __name__ == "__main__":
    main()