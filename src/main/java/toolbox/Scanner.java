package toolbox;

import java.util.regex.*;



/**
 * La classe {@link Scanner} facilite le parcours de chaines. Elle associe une chaine de caractère et une position courante.<br>
 * <br>
 * Lorsque la position courante est modifiée, aucun contrôle de validité n'est effectué. La non validité éventuelle de la position courante n'est détectée que lors de l'accès à la chaine.<br>
 * La chaine ne doit pas être <code>null</code>, mais elle peut être vide.
 * @author Ludovic WALLE
 */
public final class Scanner {



	/**
	 * Construit un nouveau {@link Scanner} avec la chaine passe en paramètre comme chaine à traiter, et 0 comme position courante.
	 * @param string La chaine à traiter (ne doit pas être <code>null</code>).
	 */
	public Scanner(String string) {
		if (string == null) {
			throw new NullPointerException();
		}
		this.string = string;
		index = 0;
	}



	/**
	 * Teste si le caractère à la position indiquée n'est aucun de ceux indiqués.
	 * @param index Position absolue du caractère à tester.
	 * @param expectedChars Caractères attendus.
	 * @return <code>true</code> si la position indiquée est valide et que le caractère à cette position n'est aucun de ceux indiqués, <code>false</code> sinon.
	 */
	public boolean absoluteCharIsNoneOf(@SuppressWarnings("hiding") int index, char... expectedChars) {
		return (index >= 0) && (index < string.length()) && charIsNoneOf(string.charAt(index), expectedChars);
	}



	/**
	 * Teste si le caractère à la position indiquée est un de ceux indiqués.
	 * @param index Position absolue du caractère à tester.
	 * @param expectedChars Caractères attendus.
	 * @return <code>true</code> si la position indiquée est valide et que le caractère à cette position est un de ceux indiqués, <code>false</code> sinon.
	 */
	public boolean absoluteCharIsOneOf(@SuppressWarnings("hiding") int index, char... expectedChars) {
		return (index >= 0) && (index < string.length()) && charIsOneOf(string.charAt(index), expectedChars);
	}



	/**
	 * Teste si le caractère à la position courante n'est aucun de ceux indiqués.
	 * @param expectedChars Caractères attendus.
	 * @return <code>true</code> si la position courante est valide et que le caractère à cette position n'est aucun de ceux indiqués, <code>false</code> sinon.
	 */
	public boolean currentCharIsNoneOf(char... expectedChars) {
		return (index >= 0) && (index < string.length()) && charIsNoneOf(string.charAt(index), expectedChars);
	}



	/**
	 * Teste si le caractère à la position courante est un de ceux indiqués.
	 * @param expectedChars Caractères attendus.
	 * @return <code>true</code> si la position courante est valide et que le caractère à cette position est un de ceux indiqués, <code>false</code> sinon.
	 */
	public boolean currentCharIsOneOf(char... expectedChars) {
		return (index >= 0) && (index < string.length()) && charIsOneOf(string.charAt(index), expectedChars);
	}



	/**
	 * Retourne le caractère à la position absolue indiquée.
	 * @param index Position absolue du caractère.
	 * @return Le caractère à la position indiquée.
	 * @throws StringIndexOutOfBoundsException Si la position courante est en dehors de la chaine.
	 */
	public char getAbsoluteChar(@SuppressWarnings("hiding") int index) throws StringIndexOutOfBoundsException {
		return string.charAt(index);
	}



	/**
	 * Retourne le nombre de caractères entre la position courante (comprise) et la fin de la chaine.<br>
	 * La valeur retournée est 0 si la position courante est égale au nombre de caractères de la chaine, et négative si elle est supérieure.
	 * @return Le nombre de caractères après la position courante.
	 */
	public int getCharsToParseCount() {
		return string.length() - index;
	}



	/**
	 * Retourne le caractère à la position courante.<br>
	 * La position courante n'est pas modifiée.
	 * @return Le caractère à la position courante.
	 * @throws StringIndexOutOfBoundsException Si la position courante est en dehors de la chaine.
	 */
	public char getCurrentChar() throws StringIndexOutOfBoundsException {
		return string.charAt(index);
	}



	/**
	 * Retourne le caractère à la position courante puis modifie la position courante de la valeur indiquée.<br>
	 * La position courante est incrémentée de 1. Elle peut se retrouver en dehors de la chaine.
	 * @param charCount Nombre de caractères.
	 * @return Le caractère à l'ancienne position courante.
	 * @throws StringIndexOutOfBoundsException Si l'ancienne position courante est en dehors de la chaine.
	 */
	public char getCurrentCharThenMove(int charCount) throws StringIndexOutOfBoundsException {
		char currentChar;

		currentChar = string.charAt(index);
		index += charCount;
		return currentChar;
	}



	/**
	 * Retourne le caractère à la position courante puis modifie la position courante de la valeur indiquée si ce caractère est un de ceux indiqués en paramètre.<br>
	 * La position courante est incrémentée de la valeur indiquée ou inchangée. Elle peut se retrouver en dehors de la chaine.
	 * @param condition Condition.
	 * @param charCount Nombre de caractères.
	 * @return Le caractère à la position courante.
	 * @throws StringIndexOutOfBoundsException Si la position courante est en dehors de la chaine.
	 */
	public char getCurrentCharThenMoveIf(boolean condition, int charCount) throws StringIndexOutOfBoundsException {
		char currentChar;

		currentChar = string.charAt(index);
		if (condition) {
			index += charCount;
		}
		return currentChar;
	}



	/**
	 * Retourne un extrait de la requête.<br>
	 * L'extrait commence au caractère en position courrante - <code>before</code> et se termine au caractère en position courante + <code>after</code>. Il est encadré par des <code>...</code> pour
	 * indiquer que l'extrait est tronqué, au début (sauf dans le cas où l'extrait atteint le début de la chaine), et à la fin (sauf dans le cas où l'extrait atteint la fin de la chaine).
	 * @param before Le nombre de caractères à extraire avant la position dans la requète.
	 * @param after Le nombre de caractères à extraire après la position dans la requète.
	 * @return Un extrait de la requète.
	 */
	public String getFragment(int before, int after) {
		return getFragment(index, before, after);
	}



	/**
	 * Retourne un extrait de la requète.<br>
	 * L'extrait commence au caractère en position <code>index - before</code> et se termine au caractère en position <code>index + after</code>. Il est encadré par des <code>...</code> pour indiquer
	 * que l'extrait est tronqué, au début (sauf dans le cas où l'extrait atteint le début de la chaine), et à la fin (sauf dans le cas où l'extrait atteint la fin de la chaine).
	 * @param index La position dans la chaine.
	 * @param before Le nombre de caractères à extraire avant la position dans la requète.
	 * @param after Le nombre de caractères à extraire après la position dans la requète.
	 * @return Un extrait de la requète.
	 */
	public String getFragment(@SuppressWarnings("hiding") int index, int before, int after) {
		int first;
		String textBefore;
		int last;
		String textAfter;

		if ((index - before) > 0) {
			first = index - before;
			textBefore = "...";
		} else {
			first = 0;
			textBefore = "";
		}
		if ((index + after) < string.length()) {
			last = index + after;
			textAfter = "...";
		} else {
			last = string.length();
			textAfter = "";
		}
		return textBefore + string.substring(first, last) + textAfter;
	}



	/**
	 * Retourne la position courante.
	 * @return La position courante.
	 */
	public int getIndex() {
		return index;
	}



	/**
	 * Retourne la partie de la chaine correspondant au premier groupe capturant du motif indiqué à partir de la position courante, ou <code>null</code> si la chaine ne correspond pas au motif.<br>
	 * La position courante est incrémentée de la longueur totale correspondant au motif.
	 * @param pattern Motif (doit être valide, et comporter au moins un groupe capturant).
	 * @return La partie de la chaine correspondant au premier groupe capturant du motif indiqué à partir de la position courante, ou <code>null</code> si la chaine ne correspond pas au motif.
	 */
	public String getMatchingPart(Pattern pattern) {
		Matcher matcher;

		if ((index >= 0) && (index < string.length()) && (matcher = pattern.matcher(string).region(index, string.length())).find() && (matcher.start() == index)) {
			index = matcher.end();
			return matcher.group(1);
		} else {
			return null;
		}
	}



	/**
	 * Retourne le caractère à la position relative indiquée.
	 * @param charCount Position relative du caractère.
	 * @return Le caractère à la position indique.
	 * @throws StringIndexOutOfBoundsException Si la position courante est en dehors de la chaine.
	 */
	public char getRelativeChar(int charCount) throws StringIndexOutOfBoundsException {
		return string.charAt(index + charCount);
	}



	/**
	 * Retourne la chaine.
	 * @return La chaine.
	 */
	public String getString() {
		return string;
	}



	/**
	 * Teste si la chaine est comlètement traitée.
	 * @return <code>true</code> si la chaine est complètement traitée, <code>false</code> sinon.
	 */
	public boolean hasCharToParse() {
		return index < string.length();
	}



	/**
	 * Change la position courante du nombre de caractères indiqué.<br>
	 * Il n'y a aucune vérification de la position courante, qui peut donc se retrouver en dehors de la chaine.
	 * @param charCount Nombre de caractères.
	 */
	public void move(int charCount) {
		index += charCount;
	}



	/**
	 * Change la position courante du nombre de caractères indiqué, si la condition est vraie.<br>
	 * Il n'y a aucune vérification de la position courante, qui peut donc se retrouver en dehors de la chaine.
	 * @param condition Condition.
	 * @param charCount Nombre de caractères.
	 * @return La valeur de la condition.
	 */
	public boolean moveIf(boolean condition, int charCount) {
		if (condition) {
			index += charCount;
		}
		return condition;
	}



	/**
	 * Change la position courante du nombre de caractères indiqué, puis retourne le caractère à cette position.<br>
	 * Il n'y a aucune vérification de la position courante, qui peut donc se retrouver en dehors de la chaine.
	 * @param charCount Nombre de caractères.
	 * @return Le caractère à la nouvelle position courante.
	 * @throws StringIndexOutOfBoundsException Si la position courante est en dehors de la chaine.
	 */
	public char moveThenGetCurrentChar(int charCount) throws StringIndexOutOfBoundsException {
		return string.charAt(index += charCount);
	}



	/**
	 * Change la position courante du nombre de caractères indiqué, puis passe tous les caractères classés comme caractères d'espacement à partir de cette position.<br>
	 * Il n'y a aucune vérification de la position courante, qui peut donc se retrouver en dehors de la chaine.
	 * @param charCount Nombre de caractères.
	 */
	public void moveThenSkipWhitespaces(int charCount) {
		index += charCount;
		while ((index >= 0) && (index < string.length()) && Character.isWhitespace(string.charAt(index))) {
			index++;
		}
	}



	/**
	 * Teste si le caractère à la position relative indiquée n'est aucun de ceux indiqués.
	 * @param charCount Position du caractère à tester, relativement à la position courante.
	 * @param expectedChars Caractères attendus.
	 * @return <code>true</code> si la position indiquée est valide et que le caractère à cette position n'est aucun de ceux indiqués, <code>false</code> sinon.
	 */
	public boolean relativeCharIsNoneOf(int charCount, char... expectedChars) {
		return absoluteCharIsNoneOf(index + charCount, expectedChars);
	}



	/**
	 * Teste si le caractère à la position relative indiquée est un de ceux indiqués.
	 * @param charCount Position du caractère à tester, relativement à la position courante.
	 * @param expectedChars Caractères attendus.
	 * @return <code>true</code> si la position indiquée est valide et que le caractère à cette position est un de ceux indiqués, <code>false</code> sinon.
	 */
	public boolean relativeCharIsOneOf(int charCount, char... expectedChars) {
		return absoluteCharIsOneOf(index + charCount, expectedChars);
	}



	/**
	 * Spécifie la position indiquée comme position courante.<br>
	 * Il n'y a aucune vérification de la position, qui peut donc se retrouver en dehors de la chaine.
	 * @param index Position.
	 */
	public void setIndex(int index) {
		this.index = index;
	}



	/**
	 * Passe tous les caractères classés comme caractères d'espacement à partir de la position courante.<br>
	 * La position courante doit être valide.<br>
	 * La position courante est incrémentée du nombre de caractères classés comme caractères d'espacement consécutifs à partir de la position courante. Elle peut se retrouver en dehors de la chaine,
	 * si il n'y avait pas d'autres caractères avant la fin de la chaine.
	 */
	public void skipWhitespaces() {
		while ((index >= 0) && (index < string.length()) && Character.isWhitespace(string.charAt(index))) {
			index++;
		}
	}



	/**
	 * Teste si le caractère indiqué n'est aucun de ceux indiqués.
	 * @param thatChar Caractère à tester.
	 * @param expectedChars Caractères attendus.
	 * @return <code>true</code> si la position courante est valide et que le caractère à cette position n'est aucun de ceux indiqués, <code>false</code> sinon.
	 */
	public static boolean charIsNoneOf(char thatChar, char... expectedChars) {
		for (char expectedChar : expectedChars) {
			if (thatChar == expectedChar) {
				return false;
			}
		}
		return true;
	}



	/**
	 * Teste si le caractère indiqué est un de ceux indiqués.
	 * @param thatChar Caractère à tester.
	 * @param expectedChars Caractères attendus.
	 * @return <code>true</code> si la position courante est valide et que le caractère à cette position est un de ceux indiqués, <code>false</code> sinon.
	 */
	public static boolean charIsOneOf(char thatChar, char... expectedChars) {
		for (char expectedChar : expectedChars) {
			if (thatChar == expectedChar) {
				return true;
			}
		}
		return false;
	}



	/**
	 * Position courante.<br>
	 * Cette position peut être en dehors de la chaine.
	 */
	private int index;



	/**
	 * Chaine à parcourir.
	 */
	private final String string;



}
