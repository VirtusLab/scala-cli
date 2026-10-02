import type {SVGProps} from 'react';
import {useId} from 'react';

type ScalaCliWordmarkProps = SVGProps<SVGSVGElement>;

/**
 * Logo mark + "Scala CLI" in the same face/weight as section headers
 * (Merriweather Variable, weight 500). Live <text> so fill tracks navbar state.
 */
export default function ScalaCliWordmark({
  className,
  ...props
}: ScalaCliWordmarkProps) {
  const uid = useId().replace(/:/g, '');
  const g = (n: number) => `sc-wm-${uid}-${n}`;

  // Cropped mark bounds from logo.svg
  const markMinX = 127.84;
  const markMinY = 82.74;
  const markContentW = 144.32;
  const markContentH = 234.52;
  const markH = 30;
  const markScale = markH / markContentH;
  const markW = markContentW * markScale;
  const gap = 11;
  const textX = markW + gap;
  // Optical vertical center for Merriweather at font-size 21 in a 30-tall mark
  const textY = markH * 0.72;
  const vbW = Math.ceil(markW + gap + 148);
  const vbH = markH;

  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox={`0 0 ${vbW} ${vbH}`}
      fill="none"
      role="img"
      aria-label="Scala CLI"
      className={className}
      {...props}
    >
      <title>Scala CLI</title>
      <defs>
        <linearGradient
          id={g(1)}
          x1="-610.33"
          y1="-3900.19"
          x2="-609.33"
          y2="-3900.19"
          gradientTransform="matrix(0, 70.98, 70.98, 0, 277020.13, 43445.54)"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0" stopColor="#5f1318" />
          <stop offset="0.3" stopColor="#5f1318" />
          <stop offset="0.78" stopColor="#7f1620" />
          <stop offset="1" stopColor="#7f1620" />
        </linearGradient>
        <linearGradient
          id={g(2)}
          x1="-610.33"
          y1="-3900.19"
          x2="-609.33"
          y2="-3900.19"
          gradientTransform="matrix(108.03, 0, 0, -108.03, 66097.93, -421155.58)"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0" stopColor="#c90d0d" />
          <stop offset="0.39" stopColor="#e3322f" />
          <stop offset="0.67" stopColor="#de3d3b" />
          <stop offset="1" stopColor="#de3d3b" />
        </linearGradient>
        <linearGradient
          id={g(3)}
          x1="-610.33"
          y1="-3900.19"
          x2="-609.33"
          y2="-3900.19"
          gradientTransform="matrix(144.33, 0, 0, -144.33, 88213.86, -562769.42)"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0" stopColor="#ba0000" />
          <stop offset="0.27" stopColor="#ca0e0e" />
          <stop offset="0.74" stopColor="#e3322f" />
          <stop offset="1" stopColor="#e3322f" />
        </linearGradient>
        <linearGradient
          id={g(4)}
          x1="-610.33"
          y1="-3900.19"
          x2="-609.33"
          y2="-3900.19"
          gradientTransform="matrix(0, -58.35, -58.35, 0, -227357.47, -35311.82)"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0" stopColor="#018845" />
          <stop offset="0.38" stopColor="#016a37" />
          <stop offset="0.76" stopColor="#1d4c36" />
          <stop offset="1" stopColor="#1d4c36" />
        </linearGradient>
        <linearGradient
          id={g(5)}
          x1="-610.33"
          y1="-3900.19"
          x2="-609.33"
          y2="-3900.19"
          gradientTransform="matrix(0, 118.23, 118.23, 0, 461324.24, 72342.46)"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0" stopColor="#006535" />
          <stop offset="0.4" stopColor="#038e47" />
          <stop offset="1" stopColor="#199e4a" />
        </linearGradient>
      </defs>

      <g
        transform={`translate(0 0) scale(${markScale}) translate(${-markMinX} ${-markMinY})`}
      >
        <polygon
          fill={`url(#${g(1)})`}
          points="272.16 154.96 127.84 118.88 127.84 173 272.16 209.08 272.16 154.96"
        />
        <polygon
          fill={`url(#${g(2)})`}
          points="272.16 155 272.16 209.02 164.13 182.01 272.16 155"
        />
        <polygon
          fill={`url(#${g(3)})`}
          points="127.84 118.82 272.16 82.74 272.16 136.86 127.84 172.94 127.84 118.82"
        />
        <polygon
          fill={`url(#${g(4)})`}
          points="127.84 263.14 272.16 227.06 272.16 281.18 127.84 317.26 127.84 263.14"
        />
        <polygon
          fill={`url(#${g(5)})`}
          points="272.16 227.06 127.84 190.98 127.84 245.1 272.16 281.18 272.16 227.06"
        />
      </g>

      <text
        className="sc-wordmark-text"
        x={textX}
        y={textY}
        fontSize={21}
        fontWeight={500}
      >
        Scala CLI
      </text>
    </svg>
  );
}
