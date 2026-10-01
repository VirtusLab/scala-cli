import UseCase from '../components/UseCase';

export default function ScriptingPage() {
  return (
    <UseCase
      title="Scripting with Scala CLI"
      description="Page describing why Scala CLI is good for scripting with Scala."
      headline="Scripting using all the powers of the Scala ecosystem"
      image="gifs/scripting.gif"
      id="scripting"
    >
      <p>
        Scala CLI allows you to use Scala to create and enhance scripts with
        using all the goodies of Scala.
      </p>

      <p>
        Use dependencies, declare tests or even package your scripts into native
        applications!
      </p>
    </UseCase>
  );
}
