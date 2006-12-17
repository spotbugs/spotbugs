package sfBugs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**

Bonjour,

je ne comprends pas le pb suivant :

"Déréférencement immédiat du résultat d'un readLine()
Le résultat d'un appel à readLine() est immédiatement déréférencé. S'il n'y a plus d'autre lignes de texte à lire, readLine() retournera null ce qui provoquera une NullPointerException lors du déréférencement."

Concerne le code suivant :
BufferedReader lReader = new BufferedReader(new InputStreamReader(in));
if ("o".equals(lReader.readLine()))

Ou est le bug ?
Qu'entendez vous exactement par "déréférencement" ?

Merci pour vos réponses


 *
 */
public class Bug1609941 {
	boolean b(File f) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(f));
		boolean result = "o".equals(in.readLine());
		in.close();
		return result;
	}

}
