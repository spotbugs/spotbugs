
CREATE TABLE `findbugs_issue` (
  `id` int(11) NOT NULL auto_increment,
  `firstSeen` datetime NOT NULL,
  `lastSeen` datetime NOT NULL,
  `hash` varchar(32) NOT NULL,
  `bugPattern` varchar(80) NOT NULL,
  `priority` int(11) NOT NULL,
  `primaryClass` varchar(512) NOT NULL,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `hash` (`hash`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `findbugs_evaluation` (
  `id` int(11) NOT NULL auto_increment,
  `issueId` int(11) NOT NULL,
  `who` varchar(64) NOT NULL,
  `designation` varchar(16) NOT NULL,
  `comment` text NOT NULL,
  `time` datetime NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `issueId` (`issueId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


CREATE TABLE `findbugs_bugreport` (
  `id` int(11) NOT NULL auto_increment,
  `hash` varchar(32) NOT NULL,
  `bugReportId` varchar(64) NOT NULL,
  `whoFiled` varchar(64) NOT NULL,
  `whenFiled` datetime NOT NULL,
  `status` varchar(64) default NULL,
  `assignedTo` varchar(64) default NULL,
  `componentId` varchar(64) default NULL,
  `componentName` varchar(128) default NULL,
  PRIMARY KEY  (`id`),
  KEY `hash` (`hash`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='Keeps track of bugs filed against FIndBugs issues' AUTO_INCREMENT=9 ;


ALTER TABLE `findbugs_evaluation`
  ADD CONSTRAINT `findbugs_evaluation_ibfk_1` FOREIGN KEY (`issueId`) REFERENCES `findbugs_issue` (`id`) ON DELETE CASCADE;

