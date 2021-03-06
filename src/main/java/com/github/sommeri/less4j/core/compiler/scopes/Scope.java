package com.github.sommeri.less4j.core.compiler.scopes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import com.github.sommeri.less4j.core.ast.ASTCssNode;
import com.github.sommeri.less4j.core.ast.AbstractVariableDeclaration;
import com.github.sommeri.less4j.core.ast.Expression;
import com.github.sommeri.less4j.core.ast.ReusableStructure;
import com.github.sommeri.less4j.core.ast.ReusableStructureName;
import com.github.sommeri.less4j.core.ast.Variable;
import com.github.sommeri.less4j.core.compiler.expressions.ExpressionFilter;

public class Scope {

  // following is used only during debugging - to generate human readable toString
  private static final String DEFAULT = "#default#";
  private static final String SCOPE = "#scope#";
  private static final String BODY_OWNER = "#body-owner#";
  private static final String DUMMY = "#dummy#";

  private String type;

  // real data
  private final ASTCssNode owner;
  private boolean presentInTree = true;
  private LocalData localData = new LocalData();
  private Stack<LocalData> localDataSnapshots = new Stack<LocalData>();

  private Scope parent;
  private List<Scope> childs = new ArrayList<Scope>();
  private List<String> names; 


  protected Scope(String type, ASTCssNode owner, List<String> names, Scope parent) {
    super();
    this.names = names;
    this.owner = owner;
    this.type = type;
    setParent(parent);
  }

  private Scope(String type, ASTCssNode owner, String name, Scope parent) {
    this(type, owner, new ArrayList<String>(Arrays.asList(name)), parent);
  }

  protected Scope(String type, ASTCssNode owner, List<String> names) {
    this(type, owner, names, null);
  }

  protected Scope(String type, ASTCssNode owner, String name) {
    this(type, owner, new ArrayList<String>(Arrays.asList(name)), null);
  }

  public void addNames(List<String> names) {
    this.names.addAll(names);
  }

  private void addChild(Scope child) {
    childs.add(child);
  }

  public Scope getParent() {
    return parent;
  }

  public List<Scope> getChilds() {
    return childs;
  }

  public List<String> getNames() {
    return names;
  }

  public boolean hasParent() {
    return getParent() != null;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(toFullName());
    result.append("\n\n").append(localData); 
    return result.toString();
  }

  private String toFullName() {
    if (hasParent())
      return getParent().toFullName() + " > " + toSimpleName();

    return toSimpleName().toString();
  }

  private String toSimpleName() {
    List<String> names = getNames();
    return "" + type + names;
  }

  public void registerVariable(AbstractVariableDeclaration declaration) {
    getLocalVariables().store(declaration);
  }

  public void registerVariable(AbstractVariableDeclaration node, Expression replacementValue) {
    getLocalVariables().store(node, replacementValue);
  }

  public void registerVariableIfNotPresent(String name, Expression replacementValue) {
    getLocalVariables().storeIfNotPresent(name, replacementValue);
  }

  public void registerVariable(String name, Expression replacementValue) {
    getLocalVariables().store(name, replacementValue);
  }

  public void fillByFilteredVariables(ExpressionFilter filter, Scope source) {
    getLocalVariables().fillByFilteredVariables(filter, source.getLocalVariables());
  }

  public void addAllMixins(List<FullMixinDefinition> mixins) {
    getLocalMixins().storeAll(mixins);
  }

  public void add(Scope otherSope) {
    getLocalMixins().storeAll(otherSope.getLocalMixins());
    getLocalVariables().storeAll(otherSope.getLocalVariables());
  }

  public Expression getValue(Variable variable) {
    return getValue(variable.getName());
  }

  public Expression getValue(String name) {
    Expression value = getLocalVariables().getValue(name);
    if (value == null && hasParent())
      return getParent().getValue(name);

    return value;
  }

  public void registerMixin(ReusableStructure mixin, Scope mixinsBodyScope) {
    getLocalMixins().store(new FullMixinDefinition(mixin, mixinsBodyScope));
  }

  public void createPlaceholder() {
    getLocalVariables().createPlaceholder();
    getLocalMixins().createPlaceholder();
  }

  public void addToPlaceholder(Scope otherScope) {
    getLocalVariables().addToPlaceholder(otherScope.getLocalVariables());
    getLocalMixins().addToPlaceholder(otherScope.getLocalMixins());
  }

  public void closePlaceholder() {
    getLocalVariables().closePlaceholder();
    getLocalMixins().closePlaceholder();
  }

  @Deprecated
  public List<FullMixinDefinition> getNearestMixins(ReusableStructureName name) {
    List<FullMixinDefinition> value = getLocalMixins().getMixins(name);
    if ((value == null || value.isEmpty()) && hasParent())
      return getParent().getNearestMixins(name);

    return value == null ? new ArrayList<FullMixinDefinition>() : value;
  }

  public List<FullMixinDefinition> getAllMixins() {
    return getLocalMixins().getAllMixins();
  }

  public List<FullMixinDefinition> getMixinsByName(ReusableStructureName name) {
    return getLocalMixins().getMixins(name);
  }

  public Scope firstChild() {
    return childs.get(0);
  }

  public boolean isBodyOwnerScope() {
    return BODY_OWNER.equals(type);
  }

  public Scope skipBodyOwner() {
    if (isBodyOwnerScope())
      return firstChild().skipBodyOwner();

    return this;
  }

  public static Scope createDefaultScope(ASTCssNode owner) {
    return new Scope(DEFAULT, owner, new ArrayList<String>());
  }

  public static Scope createScope(ASTCssNode owner, Scope parent) {
    return new Scope(SCOPE, owner, new ArrayList<String>(), parent);
  }

  public static Scope createBodyOwnerScope(ASTCssNode owner, Scope parent) {
    return new Scope(BODY_OWNER, owner, new ArrayList<String>(), parent);
  }

  public static Scope createDummyScope() {
    return new Scope(DUMMY, null, new ArrayList<String>(), null);
  }

  public static Scope createDummyScope(ASTCssNode owner, String name) {
    return new Scope(DUMMY, owner, name, null);
  }

  public String toLongString() {
    return toLongString(0).toString();
  }

  private StringBuilder toLongString(int level) {
    String prefix = "";
    for (int i = 0; i < level; i++) {
      prefix += "  ";
    }
    StringBuilder text = new StringBuilder(prefix);

    Iterator<String> iNames = getNames().iterator();
    text.append(iNames.next());
    while (iNames.hasNext()) {
      text.append(", ").append(iNames.next());
    }

    text.append("(").append(getLocalVariables().size()).append(", ").append(getLocalMixins().size());
    text.append(") {").append("\n");

    for (Scope kid : getChilds()) {
      text.append(kid.toLongString(level + 1));
    }
    text.append(prefix).append("}").append("\n");
    ;
    return text;
  }

  public Scope copyWithChildChain() {
    return copyWithChildChain(null);
  }

  public Scope copyWithChildChain(Scope parent) {
    Scope result = new Scope(type, owner, new ArrayList<String>(getNames()), parent);
    result.localData = localData;
    result.presentInTree = presentInTree;
    for (Scope kid : getChilds()) {
      kid.copyWithChildChain(result);
    }

    return result;
  }

  public Scope copyWithParentsChain() {
    Scope parent = null;
    if (hasParent()) {
      parent = getParent().copyWithParentsChain();
      for (Scope kid : getParent().getChilds())
        if (kid != this) {
          kid.copyWithChildChain(parent);
        }
    }
    Scope result = new Scope(type, owner, new ArrayList<String>(getNames()), parent);
    result.localData = localData;
    result.presentInTree = presentInTree;
    return result;
  }

  public Scope copyWholeTree() {
    Scope parentalTree = copyWithParentsChain();
    Scope parentalTreeConnector = parentalTree.getParent();
    parentalTree.setParent(null);
    Scope copyWithchildTree = copyWithChildChain(parentalTreeConnector);
    return copyWithchildTree;
  }

  public void insertAsParent(Scope parent) {
    Scope originalParent = getParent();
    parent.setParent(originalParent);
    setParent(parent);
  }

  public void setParent(Scope parent) {
    if (hasParent()) {
      getParent().getChilds().remove(this);
    }

    this.parent = parent;

    if (parent != null)
      parent.addChild(this);
  }

  public void removedFromTree() {
    presentInTree = false;
  }

  public boolean isPresentInTree() {
    return presentInTree;
  }

  public Scope getRootScope() {
    if (!hasParent())
      return this;

    return getParent().getRootScope();
  }

  public boolean seesLocalDataOf(Scope otherScope) {
    if (otherScope.localData==localData)
      return true;

    if (!hasParent())
      return false;
    
    return getParent().seesLocalDataOf(otherScope);
  }

  public Scope getChildOwnerOf(ASTCssNode body) {
    for (Scope kid : getChilds()) {
      if (kid.owner == body)
        return kid;
    }
    return null;
  }

  public Scope childByOwners(ASTCssNode headNode, ASTCssNode... restNodes) {
    Scope head = getChildOwnerOf(headNode);
    if (head == null)
      return null;

    for (ASTCssNode node : restNodes) {
      head = head.getChildOwnerOf(node);
      if (head == null)
        return null;
    }

    return head;
  }

  /**
   * Do not call this method directly. Use {@link InScopeSnapshotRunner} instead.
   */
  protected void createLocalDataSnapshot() {
    localDataSnapshots.push(localData);
    localData = localData.clone();
  }

  /**
   * Do not call this method directly. Use {@link InScopeSnapshotRunner} instead.
   */
  protected void discardLastLocalDataSnapshot() {
    localData = localDataSnapshots.pop();
  }

  class LocalData implements Cloneable {

    private VariablesDeclarationsStorage variables = new VariablesDeclarationsStorage();
    private MixinsDefinitionsStorage mixins = new MixinsDefinitionsStorage();

    @Override
    protected LocalData clone() {
      try {
        LocalData clone = (LocalData) super.clone();
        clone.variables = variables.clone();
        clone.mixins = mixins.clone();
        return clone;

      } catch (CloneNotSupportedException e) {
        throw new IllegalStateException("Impossible to happen.");
      }
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder(getClass().getSimpleName()).append("\n");
      result.append("**Variables storage: ").append(variables).append("\n\n");
      result.append("**Mixins storage: ").append(mixins).append("\n\n");
      return result.toString();
    }
  }

  private MixinsDefinitionsStorage getLocalMixins() {
    return localData.mixins;
  }

  private VariablesDeclarationsStorage getLocalVariables() {
    return localData.variables;
  }

}
