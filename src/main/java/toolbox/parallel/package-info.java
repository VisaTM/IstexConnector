package toolbox.parallel;



/**
 * Ce package regroupe des classes permettant de paralléliser l'éxécution de taches. Les classes sont calquées métaphoriquement sur une entreprise.
 * <ul>
 * <li>{@link Mission} contient les caractéristiques d'une tâche à effectuer.
 * <li>{@link Employee} regroupe les caractéristiques communes à tous les types d'employés.
 * <li>{@link Worker} est un type d'employé dont le rôle est d'effectuer les tâches qui lui sont attribuées. Il ne doit y en avoir qu'un.
 * <li>{@link Missionner} est un type d'employé dont le rôle est de distribuer les tâches aux ouvriers.
 * <li>{@link Enterprise} fait fonctionner le tout.
 * </ul>
 * Le nombre d'ouvriers est limité, souvent inférieur au nombre de tâches à effectuer, mais ils travaillent en paralléle, chacun sur sa tâche.<br>
 * L'entreprise ferme lorsque toutes les tâches ont été effectuées. Il est possible de suspendre temporairement l'entreprise, de la fermer volontairement, d'embaucher et de débaucher
 * dynamiquement des ouvriers, de suivre l'état d'avancement du travail.<br>
 * <br>
 * Il faut dériver les classes {@link Mission}, {@link Missionner} et {@link Worker} pour les adapter au travail à réaliser.
 * @author Ludovic WALLE
 */
