import { useState } from 'react';

type Question = {
  number: number;
  topic: string;
  text: string;
};

function App() {
  const [topic, setTopic] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [output, setOutput] = useState('');
  const [loading, setLoading] = useState(false);

  const generateFromTopic = async () => {
    if (!topic.trim()) {
      setOutput('Please enter a topic first.');
      return;
    }

    setLoading(true);
    setOutput('Generating questions from topic...');
    setQuestions([]);

    try {
      const response = await fetch(
        `http://localhost:8080/api/ai/generate?topic=${encodeURIComponent(topic)}`
      );
      if (!response.ok) throw new Error('Backend request failed');
      const data = await response.json();
      if (data.questions && Array.isArray(data.questions)) {
        setQuestions(data.questions);
        setOutput('');
      } else {
        setOutput('No questions found.');
      }
    } catch (err: any) {
      setOutput('Error: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const generateFromPdf = async () => {
    if (!file) {
      setOutput('Please choose a PDF first.');
      return;
    }
    if (!topic.trim()) {
      setOutput('Please enter a topic (e.g. "GCSE Biology").');
      return;
    }

    setLoading(true);
    setOutput('Generating questions from PDF notes...');
    setQuestions([]);

    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('topic', topic);

      const response = await fetch('http://localhost:8080/api/ai/generateFromPdf', {
        method: 'POST',
        body: formData,
      });
      if (!response.ok) throw new Error('Backend request failed');
      const data = await response.json();
      if (data.questions && Array.isArray(data.questions)) {
        setQuestions(data.questions);
        setOutput('');
      } else if (data.error) {
        setOutput('Error: ' + data.error);
      } else {
        setOutput('No questions found.');
      }
    } catch (err: any) {
      setOutput('Error: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        padding: '40px',
        fontFamily: 'Arial, sans-serif',
        maxWidth: '800px',
        margin: 'auto',
        color: 'white',
      }}
    >
      <h1>AI Question Generator</h1>

      <p>Enter a topic and optionally upload your notes (PDF):</p>
      <input
        value={topic}
        onChange={(e) => setTopic(e.target.value)}
        placeholder='e.g. Geography, Biology, Algorithms...'
        style={{
          width: '100%',
          padding: '10px',
          fontSize: '1rem',
          borderRadius: '6px',
          marginBottom: '15px',
        }}
      />

      <input
        type="file"
        accept="application/pdf"
        onChange={(e) => setFile(e.target.files?.[0] || null)}
        style={{ marginBottom: '15px', display: 'block' }}
      />

      <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
        <button
          onClick={generateFromTopic}
          disabled={loading}
          style={{
            padding: '10px 20px',
            fontSize: '1rem',
            cursor: 'pointer',
            borderRadius: '6px',
          }}
        >
          {loading ? 'Working...' : 'Generate from Topic'}
        </button>

        <button
          onClick={generateFromPdf}
          disabled={loading}
          style={{
            padding: '10px 20px',
            fontSize: '1rem',
            cursor: 'pointer',
            borderRadius: '6px',
          }}
        >
          {loading ? 'Working...' : 'Generate from PDF'}
        </button>
      </div>

      <h2 style={{ marginTop: '30px' }}>Output:</h2>

      {output && (
        <pre
          style={{
            background: '#f5f5f5',
            color: 'black',
            border: '1px solid #ccc',
            padding: '15px',
            whiteSpace: 'pre-wrap',
            minHeight: '100px',
          }}
        >
          {output}
        </pre>
      )}

      {questions.length > 0 && (
        <div style={{ marginTop: '20px' }}>
          {questions.map((q) => (
            <div
              key={q.number}
              style={{
                background: '#f5f5f5',
                color: 'black',
                border: '1px solid #ccc',
                borderRadius: '8px',
                padding: '15px',
                marginBottom: '15px',
              }}
            >
              <h3>
                Question {q.number}: {q.topic}
              </h3>
              <p>{q.text}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default App;
