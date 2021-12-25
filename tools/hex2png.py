
import csv
import sys
import uuid

# Allow larger fields
csv.field_size_limit(2000000)

def flatten(t):
    return [item for sublist in t for item in sublist]

with open('images.csv', newline='') as csvfile:
	csv = csv.reader(csvfile)
	data = flatten(list(csv))
	print('converting:', len(data), 'images')
	
	for image in data:
		filename = str(uuid.uuid4())

		# skip leading 0x
		bytes = bytearray.fromhex(image[2:])
		
		with open('output/' + filename + ".png", "wb") as f:
			f.write(bytes)

		print('saved', filename + ".png")
	
	print('converted:', len(data), 'images to output/')
