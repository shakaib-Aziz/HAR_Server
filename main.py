from flask import Flask, request, jsonify
import os
import csv

app = Flask(__name__)

DATA_DIR = 'sensor_data'

def save_data(data):
    user_id = data.get('user_id', 'unknown_user')
    source = data.get('source', 'unknown_source')

    user_dir = os.path.join(DATA_DIR, source, user_id)
    os.makedirs(user_dir, exist_ok=True)

    filename = f"{user_id}.csv"
    filepath = os.path.join(user_dir, filename)

    headers = ['timestamp', 'sensor_type', 'x', 'y', 'z', 'user_id']

    # Check if the CSV file exists, if not, create it with headers
    if not os.path.exists(filepath):
        with open(filepath, 'w', newline='') as file:
            writer = csv.writer(file)
            writer.writerow(headers)

    # Append the sensor data to the CSV file
    with open(filepath, 'a', newline='') as file:
        writer = csv.writer(file)
        writer.writerow([data[field] for field in headers])

@app.route('/sensor-data', methods=['POST'])
def receive_sensor_data():
    print("Connection estavlished")
    data = request.json

    # Validate received data
    required_fields = ['source','user_id','timestamp', 'sensor_type', 'x', 'y', 'z']
    if not all(field in data for field in required_fields):
        print({'error': 'Invalid data format'})
        return jsonify({'error': 'Invalid data format'}), 400

    # Save the data to the appropriate directory
    save_data(data)

    return jsonify({'message': 'Data received successfully'}), 200

if __name__ == '__main__':
    app.run(debug=True, ssl_context='adhoc')

#, host='192.168.1.101', port=9000
