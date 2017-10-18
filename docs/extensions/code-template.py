import os

def generate(release):
    for filename in os.listdir('code-template'):
        with open('code-template/' + filename, 'r') as template:
            data = template.read()
            with open('generated/' + filename + '.inc', 'w') as generated:
                generated.write(data.replace('|release|', release))

def setup(app):
    app.connect('builder-inited', lambda app: generate(app.config.release))
