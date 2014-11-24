package sfBugsNew;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug1295jdk8 {

    public List<AncestorNodeReference> getAncestors() {
        List<AncestorNodeReference> ancestorNodes;
        
        ancestorNodes = new ArrayList<AncesterNodeReferenceDTO>()
                .stream()
                .map(this::createAncestorNodeReference)
                .collect(java.util.stream.Collectors.toList());

        return ancestorNodes;
    }

    @DesireNoWarning("UPM_UNCALLED_PRIVATE_METHOD")
    private AncestorNodeReference createAncestorNodeReference(final AncesterNodeReferenceDTO ancestorNodeRefDTO) {
        return createAncestorNodeReference("");
    }

    private AncestorNodeReference createAncestorNodeReference(String bla) {
        return null;
    }
}

class AncestorNodeReference {
}

class AncesterNodeReferenceDTO {
}