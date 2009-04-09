--
-- Database: `findbugs`
--

-- --------------------------------------------------------

--
-- Table structure for table `findbugsIssues`
--

CREATE TABLE `findbugs_issues` (
  `id` int(11) NOT NULL auto_increment,
  `firstSeen` date NOT NULL,
  `lastSeen` date NOT NULL,
  `hash` varchar(32) NOT NULL,
  `bugPattern` varchar(80) NOT NULL,
  `priority` int(11) NOT NULL,
  `primaryClass` varchar(512) NOT NULL,
  `bugDatabaseKey` varchar(64) NOT NULL,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `hash` (`hash`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=1514 ;


CREATE TABLE `findbugs_evaluations` (
  `id` int(11) NOT NULL auto_increment,
  `issueId` int(11) NOT NULL,
  `who` varchar(16) NOT NULL,
  `designation` varchar(16) NOT NULL,
  `comment` text NOT NULL,
  `when` date NOT NULL,
  KEY `issueId` (`issueId`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

