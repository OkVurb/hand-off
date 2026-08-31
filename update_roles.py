import json
import glob
for file in glob.glob('src/main/resources/data/planeshift/planeshift/role/*.json'):
    with open(file, 'r') as f:
        data = json.load(f)
    data['jump_multiplier'] = round(data['jump_multiplier'] * 1.5, 2)
    with open(file, 'w') as f:
        json.dump(data, f, indent=2)