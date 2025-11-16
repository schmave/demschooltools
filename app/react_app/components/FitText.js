import React from 'react';
import Box from './Box';
import Typography from './Typography';
import { combineSx } from '../utils';

const useNormalizedBounds = (maxSize, minSize) =>
  React.useMemo(() => {
    const safeMax = Number.isFinite(maxSize) ? Math.max(1, maxSize) : 32;
    const safeMin = Number.isFinite(minSize)
      ? Math.max(1, Math.min(minSize, safeMax))
      : Math.min(12, safeMax);

    return { min: safeMin, max: safeMax };
  }, [maxSize, minSize]);

const useNormalizedRows = (maxRows) =>
  React.useMemo(() => {
    if (!Number.isFinite(maxRows)) {
      return 1;
    }
    return Math.max(1, Math.floor(maxRows));
  }, [maxRows]);

const useNumericLineHeight = (lineHeight) =>
  React.useMemo(() => {
    if (typeof lineHeight === 'number' && Number.isFinite(lineHeight)) {
      return Math.max(0.5, lineHeight);
    }
    const parsed = parseFloat(lineHeight);
    return Number.isFinite(parsed) ? Math.max(0.5, parsed) : 1.1;
  }, [lineHeight]);

const FitText = ({
  children,
  maxSize = 32,
  minSize = 8,
  maxRows = 1,
  lineHeight = 1.1,
  containerSx,
  containerProps = {},
  sx,
  ...typographyProps
}) => {
  const containerRef = React.useRef(null);
  const textRef = React.useRef(null);
  const { min: minBound, max: maxBound } = useNormalizedBounds(maxSize, minSize);
  const normalizedMaxRows = useNormalizedRows(maxRows);
  const numericLineHeight = useNumericLineHeight(lineHeight);
  const [fontSize, setFontSize] = React.useState(maxBound);
  const fontSizeRef = React.useRef(maxBound);
  const [isWrapped, setIsWrapped] = React.useState(false);
  const wrapRef = React.useRef(false);
  const resizeFrame = React.useRef(null);
  const allowWrap = typographyProps.noWrap !== true && normalizedMaxRows > 1;

  React.useEffect(() => {
    fontSizeRef.current = fontSize;
  }, [fontSize]);

  React.useEffect(() => {
    wrapRef.current = isWrapped;
  }, [isWrapped]);

  React.useEffect(() => {
    return () => {
      if (
        resizeFrame.current !== null &&
        typeof window !== 'undefined' &&
        typeof window.cancelAnimationFrame === 'function'
      ) {
        window.cancelAnimationFrame(resizeFrame.current);
        resizeFrame.current = null;
      }
    };
  }, []);

  React.useEffect(() => {
    setFontSize((current) => {
      const clamped = Math.min(Math.max(current, minBound), maxBound);
      fontSizeRef.current = clamped;
      return clamped;
    });
  }, [minBound, maxBound]);

  const measure = React.useCallback(() => {
    const container = containerRef.current;
    const text = textRef.current;

    if (!container || !text) {
      return;
    }

    const widthLimit = container.clientWidth;
    if (widthLimit === 0 || container.clientHeight === 0) {
      return;
    }

    const runBinarySearch = (wrapEnabled) => {
      text.style.whiteSpace = wrapEnabled ? 'normal' : 'nowrap';
      text.style.wordBreak = wrapEnabled ? 'break-word' : 'normal';
      text.style.overflowWrap = wrapEnabled ? 'anywhere' : 'normal';

      let low = minBound;
      let high = maxBound;
      let best = minBound;
      let fitsAny = false;

      const checkOverflow = (size) => {
        const horizontalOverflow = text.scrollWidth - widthLimit > 0.5;
        const maxLineCount = wrapEnabled ? normalizedMaxRows : 1;
        const maxHeight =
          numericLineHeight * size * maxLineCount;
        const heightOverflow =
          wrapEnabled && Number.isFinite(maxHeight)
            ? text.scrollHeight - maxHeight > 0.5
            : false;

        return horizontalOverflow || heightOverflow;
      };

      while (low <= high) {
        const mid = Math.floor((low + high) / 2);
        text.style.fontSize = `${mid}px`;
        const overflow = checkOverflow(mid);

        if (overflow) {
          high = mid - 1;
        } else {
          fitsAny = true;
          best = mid;
          low = mid + 1;
        }
      }

      const finalSize = fitsAny ? best : minBound;
      text.style.fontSize = `${finalSize}px`;
      const finalOverflow = checkOverflow(finalSize);

      return {
        size: finalSize,
        fits: !finalOverflow,
      };
    };

    const singleLineResult = runBinarySearch(false);

    let targetSize = singleLineResult.size;
    let shouldWrap = false;

    if (allowWrap) {
      const wrapResult = runBinarySearch(true);

      if (
        wrapResult.fits &&
        (!singleLineResult.fits || wrapResult.size > singleLineResult.size)
      ) {
        targetSize = wrapResult.size;
        shouldWrap = true;
      } else {
        text.style.whiteSpace = 'nowrap';
        text.style.wordBreak = 'normal';
        text.style.overflowWrap = 'normal';
        text.style.fontSize = `${singleLineResult.size}px`;
      }
    }

    const clampedTarget = Math.min(Math.max(targetSize, minBound), maxBound);

    if (clampedTarget !== fontSizeRef.current || shouldWrap !== wrapRef.current) {
      fontSizeRef.current = clampedTarget;
      wrapRef.current = shouldWrap;
      setFontSize(clampedTarget);
      setIsWrapped(shouldWrap);
    } else {
      text.style.fontSize = `${clampedTarget}px`;
      text.style.whiteSpace = shouldWrap ? 'normal' : 'nowrap';
      text.style.wordBreak = shouldWrap ? 'break-word' : 'normal';
      text.style.overflowWrap = shouldWrap ? 'anywhere' : 'normal';
    }
  }, [allowWrap, minBound, maxBound, normalizedMaxRows, numericLineHeight]);

  const scheduleMeasure = React.useCallback(() => {
    const canUseRaf =
      typeof window !== 'undefined' &&
      typeof window.requestAnimationFrame === 'function' &&
      typeof window.cancelAnimationFrame === 'function';

    if (!canUseRaf) {
      measure();
      return;
    }

    if (resizeFrame.current !== null) {
      window.cancelAnimationFrame(resizeFrame.current);
    }
    resizeFrame.current = window.requestAnimationFrame(() => {
      resizeFrame.current = null;
      measure();
    });
  }, [measure]);

  React.useLayoutEffect(() => {
    scheduleMeasure();
  }, [scheduleMeasure, children, allowWrap, lineHeight, normalizedMaxRows]);

  React.useEffect(() => {
    const container = containerRef.current;
    if (!container || typeof ResizeObserver === 'undefined') {
      return;
    }

    const observer = new ResizeObserver(() => {
      scheduleMeasure();
    });

    observer.observe(container);
    return () => observer.disconnect();
  }, [scheduleMeasure]);

  const { sx: containerPropsSx, ...restContainerProps } = containerProps;

  const resolvedContainerSx = combineSx(
    {
      position: 'relative',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      overflow: 'hidden',
      width: '100%',
      height: '100%',
      whiteSpace: isWrapped ? 'normal' : 'nowrap',
    },
    containerSx,
    containerPropsSx,
  );

  const resolvedTypographySx = combineSx(
    {
      fontSize: 'inherit',
      lineHeight: 'inherit',
      width: '100%',
      margin: 0,
      display: 'block',
    },
    sx,
  );

  const textAlign = typographyProps.align ?? 'inherit';

  return (
    <Box ref={containerRef} sx={resolvedContainerSx} {...restContainerProps}>
      <Box
        ref={textRef}
        sx={{
          fontSize: `${fontSize}px`,
          lineHeight,
          width: '100%',
          display: 'block',
          textAlign,
        }}
      >
        <Typography {...typographyProps} sx={resolvedTypographySx}>
          {children}
        </Typography>
      </Box>
    </Box>
  );
};

export default FitText;
