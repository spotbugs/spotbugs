import os

def generate(config):
    release = config.release
    maven = config.html_context['maven_plugin_version']
    gradle = config.html_context['gradle_plugin_version']
    archetype = config.html_context['archetype_version']
    for filename in os.listdir('code-template'):
        with open('code-template/' + filename, 'r') as template:
            data = template.read()
            with open('generated/' + filename + '.inc', 'w') as generated:
                generated.write(data.replace('|release|', release).replace('|maven-plugin|', maven).replace('|gradle-plugin|', gradle).replace('|archetype|', archetype))

def setup(app):
    app.connect('builder-inited', lambda app: generate(app.config))
