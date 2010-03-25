# See http://code.google.com/p/findbugs/wiki/AppEngineCloudBackup

from google.appengine.ext import db
from google.appengine.tools import bulkloader
import datetime

#============================== DbUser ==============================

class AppEngineDbUser(db.Model):
    email = db.StringProperty()

class AppEngineDbUserLoader(bulkloader.Loader):
    def __init__(self):
        bulkloader.Loader.__init__(self, 'AppEngineDbUser',
                                   [('openid', str),
                                    ('email', str)
                                   ])
    def handle_entity(self, entity):
        entity.openid = None
        return entity
    def generate_key(self, i, values):
        return values[0]

class AppEngineDbUserExporter(bulkloader.Exporter):
    def __init__(self):
        bulkloader.Exporter.__init__(self, 'AppEngineDbUser',
                                     [ ('__key__', lambda x: x.to_path()[1], ''),
                                      ('email', str, '')
                                     ])

#=========================== DbInvocation ============================

class AppEngineDbInvocation(db.Model):
    who = db.ReferenceProperty(AppEngineDbUser)
    startTime = db.IntegerProperty()
    endTime = db.IntegerProperty()

class AppEngineDbInvocationLoader(bulkloader.Loader):
    def __init__(self):
        bulkloader.Loader.__init__(self, 'AppEngineDbInvocation',
                                   [('pk', int),
                                    ('who', lambda x: db.Key.from_path('AppEngineDbUser', x)),
                                    ('startTime', long),
                                    ('endTime', long)
                                   ])
    def handle_entity(self, entity):
        entity.pk = None
        return entity
    def generate_key(self, i, values):
        return int(values[0])

class AppEngineDbInvocationExporter(bulkloader.Exporter):
    def __init__(self):
        bulkloader.Exporter.__init__(self, 'AppEngineDbInvocation',
                                     [ ('__key__', lambda x: x.to_path()[1], ''),
                                       ('who', lambda x: x.to_path()[1], ''),
                                       ('startTime', str, ''),
                                       ('endTime', str, '')
                                     ])


#============================= DbIssue ==============================

class AppEngineDbIssue(db.Model):
    bugPattern = db.StringProperty()
    priority = db.IntegerProperty()
    primaryClass = db.StringProperty()
    firstSeen = db.IntegerProperty()
    lastSeen = db.IntegerProperty()
    bugLink = db.StringProperty()
    bugLinkType = db.StringProperty()

class AppEngineDbIssueLoader(bulkloader.Loader):
    def __init__(self):
        bulkloader.Loader.__init__(self, 'AppEngineDbIssue',
                                   [('issueHash', str),
                                    ('bugPattern', str),
                                    ('priority', int),
                                    ('primaryClass', str),
                                    ('firstSeen', long),
                                    ('lastSeen', long),
                                    ('bugLink', str),
                                    ('bugLinkType', str)
                                   ])
    def handle_entity(self, entity):
        entity.issueHash = None
        if (entity.bugLink == 'None'): entity.bugLink = None
        if (entity.bugLinkType == 'None'): entity.bugLinkType = None

        return entity
    def generate_key(self, i, values):
        return db.Key.from_path('AppEngineDbInvocation', int(values[0]))

class AppEngineDbIssueExporter(bulkloader.Exporter):
    def __init__(self):
        bulkloader.Exporter.__init__(self, 'AppEngineDbIssue',
                                     [('__key__', lambda x: x.to_path()[1], ''),
                                      ('bugPattern', str, ''),
                                      ('priority', str, ''),
                                      ('primaryClass', str, ''),
                                      ('firstSeen', str, ''),
                                      ('lastSeen', str, ''),
                                      ('bugLink', str, ''),
                                      ('bugLinkType', str, '')
                                     ])

#============================ DbEvaluation ==============================

class AppEngineDbEvaluation(db.Model):
    who = db.ReferenceProperty(AppEngineDbUser)
    designation = db.StringProperty()
    comment = db.StringProperty()
    when = db.IntegerProperty()
    invocation  = db.ReferenceProperty(AppEngineDbInvocation)

class AppEngineDbEvaluationLoader(bulkloader.Loader):
    def __init__(self):
        bulkloader.Loader.__init__(self, 'AppEngineDbEvaluation',
                                   [('who', str),
                                   ('designation', str),
                                   ('comment', str),
                                   ('issueHash', str),
                                   ('when', long),
                                   ('invocation', db.Key)
                                   ])
    def handle_entity(self, entity):
        entity.issueHash = None
        return entity
    def generate_key(self, i, values):
        return values[3]

class AppEngineDbEvaluationExporter(bulkloader.Exporter):
    def __init__(self):
        bulkloader.Exporter.__init__(self, 'AppEngineDbEvaluation',
                                     [ ('who', lambda x: x.to_path()[1], ''),
                                       ('designation', str, ''),
                                       ('comment', str, ''),
                                       ('__key__', lambda x: x.to_path()[1], ''),
                                       ('when', str, ''),
                                       ('invocation', lambda x: x.to_path()[1], ''),
                                     ])

#============================ initialization ==============================

loaders = [AppEngineDbUserLoader, AppEngineDbIssueLoader, AppEngineDbEvaluationLoader, AppEngineDbInvocationLoader]

exporters = [AppEngineDbUserExporter, AppEngineDbIssueExporter, AppEngineDbEvaluationExporter, AppEngineDbInvocationExporter]