#! /usr/bin/perl

use strict qw(refs vars);
use FileHandle;
use Getopt::Std;

my %opts = ();
if (!getopts("b", \%opts)) {
	Usage();
}

my @imageExtensionList = (
	".pdf", ".png"
);

foreach my $file (@ARGV) {
	my @depList = ();

	my @workList = ();
	push @workList, $file;

	my %seen = ();

	while (scalar(@workList) > 0) {
		my $src = shift @workList;

		my $fh = new FileHandle("<$src");
		(defined $fh) || die "Couldn't open $src: $!\n";

		while (<$fh>) {
			my $dep = undef;

			if (/^[^%]*\\(input|include)\{([^}]+)\}/) {
				$dep = $2;
				if (!($dep =~ /\.tex$/)) {
					$dep .= ".tex";
				}
			} elsif (/^[^%]*\\includegraphics(\[width=[^\]]+\])?\{([^}]+)\}/) {
				#print "Image: $2\n";
				$dep = Resolve_Image($2, $src);
			}

			if ((defined $dep) && (!$seen{$dep})) {
				push @depList, $dep;
				if ($dep =~ /\.tex$/) {
					push @workList, $dep;
				}
				$seen{$dep} = 1;
			}
		}

		$fh->close();
	}

	my $outfile = $file;
	$outfile =~ s,\.tex$,.pdf,;

	if ($opts{'b'} && $file =~ /^(.*)\.tex$/) {
		my $bibfile = "$1.bib";
		if (-r $bibfile) {
			push @depList, $bibfile;
		}
	}

	print "$outfile : $file ", join(' ', @depList), "\n\n";
}

sub Resolve_Image {
	my ($img, $src) = @_;
	my $found = 0;
	if (!($img =~ /\.[A-Za-z0-9]+$/)) {
		foreach my $ext (@imageExtensionList) {
			if (-r "$img$ext") {
				$img = "$img$ext";
				#print STDERR "Found $img referenced from $src\n";
				$found = 1;
				last;
			}
		}
	} else {
		$found = (-r $img);
	}
	die "Couldn't resolve image $img from $src\n" if (!$found);
	return $img;
}

sub Usage {
	print STDERR "Usage: mkdep.pl [-b] <tex sources...>\n";
	print STDERR "Options:\n";
	print STDERR "  -b    Check for .bib file matching main source file\n";
	exit 1
}

# vim:ts=4
