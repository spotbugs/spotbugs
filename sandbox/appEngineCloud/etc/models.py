# See http://code.google.com/p/findbugs/wiki/AppEngineCloudBackup

from google.appengine.ext import db
from google.appengine.tools import bulkloader
import datetime

class DbUser(db.Model):
    email = db.StringProperty()

class DbUserLoader(bulkloader.Loader):
    def __init__(self):
        bulkloader.Loader.__init__(self, 'DbUser',
                                   [('email', str)
                                   ])

class DbUserExporter(bulkloader.Exporter):
    def __init__(self):
        bulkloader.Exporter.__init__(self, 'DbUser',
                                     [ ('__key__', lambda x: x.to_path()[1], None),
                                      ('email', str, None)
                                     ])

class DbInvocation(db.Model):
    who = db.ReferenceProperty(DbUser)
    startTime = db.IntegerProperty()
    endTime = db.IntegerProperty()

class DbInvocationLoader(bulkloader.Loader):
    def __init__(self):
        bulkloader.Loader.__init__(self, 'DbInvocation',
                                   [('who', db.Key),
                                   ('startTime', long),
                                   ('endTime', long)
                                   ])

class DbInvocationExporter(bulkloader.Exporter):
    def __init__(self):
        bulkloader.Exporter.__init__(self, 'DbInvocation',
                                     [ ('who', lambda x: x.to_path()[1], None),
                                       ('startTime', str, None),
                                       ('endTime', str, '')
                                     ])


class DbIssue(db.Model):
    bugPattern = db.StringProperty()
    priority = db.IntegerProperty()
    primaryClass = db.StringProperty()
    firstSeen = db.IntegerProperty()
    lastSeen = db.IntegerProperty()
    bugLink = db.StringProperty()

class DbIssueLoader(bulkloader.Loader):
    def __init__(self):
        bulkloader.Loader.__init__(self, 'DbIssue',
                                   [('bugPattern', str),
                                   ('priority', int),
                                   ('primaryClass', str),
                                   ('firstSeen', long),
                                   ('lastSeen', long),
                                   ('bugLink', str)
                                   ])

class DbIssueExporter(bulkloader.Exporter):
    def __init__(self):
        bulkloader.Exporter.__init__(self, 'DbIssue',
                                     [ ('__key__', lambda x: x.to_path()[1], None),
                                      ('bugPattern', str, None),
                                      ('priority', str, None),
                                      ('primaryClass', str, None),
                                      ('firstSeen', str, None),
                                      ('lastSeen', str, 0),
                                      ('bugLink', str, '')
                                     ])
class DbEvaluation(db.Model):
    who = db.ReferenceProperty(DbUser)
    designation = db.StringProperty()
    comment = db.StringProperty()
    when = db.IntegerProperty()
    invocation  = db.ReferenceProperty(DbInvocation)

class DbEvaluationLoader(bulkloader.Loader):
    def __init__(self):
        bulkloader.Loader.__init__(self, 'DbEvaluation',
                                   [('who', str),
                                   ('designation', str),
                                   ('comment', str),
                                   ('when', long),
                                   ('invocation', db.Key)
                                   ])

class DbEvaluationExporter(bulkloader.Exporter):
    def __init__(self):
        bulkloader.Exporter.__init__(self, 'DbEvaluation',
                                     [ ('who', lambda x: x.to_path()[1], None),
                                       ('designation', str, ''),
                                       ('comment', str, ''),
                                       ('__key__', lambda x: x.to_path()[1], None),
                                       ('when', str, None),
                                       ('invocation', lambda x: x.to_path()[1], None),
                                     ])


loaders = [DbUserLoader, DbIssueLoader, DbEvaluationLoader, DbInvocationLoader]

exporters = [DbUserExporter, DbIssueExporter, DbEvaluationExporter, DbInvocationExporter]